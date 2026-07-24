"""SmartAgents 数据库层 — SQLite + OffByOne 账号体系 + 邮箱验证码"""
import sqlite3
import secrets
import hashlib
import time
from pathlib import Path
from typing import Optional

DB_DIR = Path(__file__).parent / "data"
DB_PATH = DB_DIR / "smartagents.db"


def get_db() -> sqlite3.Connection:
    DB_DIR.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    return conn


def init_db():
    conn = get_db()
    conn.executescript("""
        -- SmartAgents 桌面端用户（4位凭证）
        CREATE TABLE IF NOT EXISTS users (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            username   TEXT UNIQUE NOT NULL,
            password   TEXT NOT NULL,
            phone      TEXT UNIQUE,
            email      TEXT DEFAULT '',
            token      TEXT UNIQUE,
            token_ts   REAL DEFAULT 0,
            created_at REAL NOT NULL DEFAULT (strftime('%s','now')),
            last_login REAL DEFAULT 0,
            status     TEXT DEFAULT 'active'
        );

        -- OffByOne 平台用户（邮箱+密码）
        CREATE TABLE IF NOT EXISTS offbyone_users (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            email        TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            sa_username  TEXT,
            sa_token     TEXT,
            created_at   REAL NOT NULL DEFAULT (strftime('%s','now')),
            last_login   REAL DEFAULT 0
        );

        -- 邮箱验证码
        CREATE TABLE IF NOT EXISTS verification_codes (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            email      TEXT NOT NULL,
            code       TEXT NOT NULL,
            expires_at REAL NOT NULL,
            used       INTEGER DEFAULT 0,
            created_at REAL NOT NULL DEFAULT (strftime('%s','now'))
        );

        CREATE TABLE IF NOT EXISTS waitlist (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            phone        TEXT UNIQUE NOT NULL,
            email        TEXT DEFAULT '',
            queue_number INTEGER NOT NULL,
            created_at   REAL NOT NULL DEFAULT (strftime('%s','now')),
            notified     INTEGER DEFAULT 0
        );

        CREATE TABLE IF NOT EXISTS rate_limits (
            key        TEXT PRIMARY KEY,
            count      INTEGER DEFAULT 1,
            window_start REAL NOT NULL
        );

        CREATE TABLE IF NOT EXISTS login_log (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            username   TEXT NOT NULL,
            ip         TEXT DEFAULT '',
            success    INTEGER DEFAULT 0,
            created_at REAL NOT NULL DEFAULT (strftime('%s','now'))
        );
    """)
    conn.commit()
    conn.close()


def _hash_pwd(pwd: str) -> str:
    return hashlib.sha256(f"sa:{pwd}:salt".encode()).hexdigest()[:32]


def generate_credentials() -> tuple[str, str]:
    """生成 4 位用户名和 4 位密码"""
    username = str(secrets.randbelow(9000) + 1000)
    password = str(secrets.randbelow(9000) + 1000)
    return username, password


def generate_token() -> str:
    return secrets.token_hex(32)


# ══════════════════════════════════════════
# 邮箱验证码
# ══════════════════════════════════════════

def store_verification_code(email: str) -> str:
    """生成 6 位验证码并存储，返回验证码字符串。有效期 5 分钟，同邮箱旧码自动标记已用"""
    conn = get_db()
    try:
        # 标记旧码
        conn.execute(
            "UPDATE verification_codes SET used=1 WHERE email=? AND used=0",
            (email,)
        )
        code = str(secrets.randbelow(900000) + 100000)
        expires_at = time.time() + 300  # 5 分钟有效
        conn.execute(
            "INSERT INTO verification_codes (email, code, expires_at) VALUES (?,?,?)",
            (email, code, expires_at)
        )
        conn.commit()
        return code
    finally:
        conn.close()


def verify_code(email: str, code: str) -> bool:
    """校验验证码，成功返回 True 并标记已用，失败返回 False"""
    conn = get_db()
    try:
        row = conn.execute(
            "SELECT id, expires_at FROM verification_codes WHERE email=? AND code=? AND used=0 ORDER BY id DESC LIMIT 1",
            (email, code)
        ).fetchone()
        if not row:
            return False
        if time.time() > row["expires_at"]:
            return False
        conn.execute("UPDATE verification_codes SET used=1 WHERE id=?", (row["id"],))
        conn.commit()
        return True
    finally:
        conn.close()


# ══════════════════════════════════════════
# OffByOne 账号体系
# ══════════════════════════════════════════

def create_offbyone_user(email: str, password: str) -> Optional[dict]:
    """创建 OffByOne 账号。返回 {email, created} 或 None（邮箱已存在）"""
    conn = get_db()
    try:
        existing = conn.execute(
            "SELECT id FROM offbyone_users WHERE email=?", (email,)
        ).fetchone()
        if existing:
            return None

        pwd_hash = _hash_pwd(password)
        token = generate_token()
        conn.execute(
            "INSERT INTO offbyone_users (email, password_hash, sa_token) VALUES (?,?,?)",
            (email, pwd_hash, token)
        )
        conn.commit()
        return {"email": email, "token": token}
    finally:
        conn.close()


def login_offbyone_user(email: str, password: str) -> Optional[dict]:
    """OffByOne 登录。成功返回 {email, token, has_sa_credentials}，失败返回 None"""
    conn = get_db()
    try:
        user = conn.execute(
            "SELECT id, password_hash, sa_username, sa_token FROM offbyone_users WHERE email=?",
            (email,)
        ).fetchone()
        if not user:
            return None
        if user["password_hash"] != _hash_pwd(password):
            return None

        # 刷新 token
        new_token = generate_token()
        conn.execute(
            "UPDATE offbyone_users SET sa_token=?, last_login=? WHERE id=?",
            (new_token, time.time(), user["id"])
        )
        conn.commit()

        return {
            "email": email,
            "token": new_token,
            "has_sa_credentials": user["sa_username"] is not None,
            "sa_username": user["sa_username"],
        }
    finally:
        conn.close()


def verify_offbyone_token(token: str) -> Optional[dict]:
    """验证 OffByOne token"""
    conn = get_db()
    try:
        user = conn.execute(
            "SELECT email, sa_username FROM offbyone_users WHERE sa_token=?",
            (token,)
        ).fetchone()
        if not user:
            return None
        return {"email": user["email"], "sa_username": user["sa_username"]}
    finally:
        conn.close()


def apply_sa_credentials(token: str) -> Optional[dict]:
    """
    已登录 OffByOne 用户申请 SmartAgents 内测凭证。
    若该用户已有凭证则直接返回；否则生成新的 4 位凭证，同时写入 users 表和 offbyone_users 表。
    返回 {username, password, sa_token} 或 None（token 无效）
    """
    conn = get_db()
    try:
        ob_user = conn.execute(
            "SELECT id, email, sa_username, sa_token FROM offbyone_users WHERE sa_token=?",
            (token,)
        ).fetchone()
        if not ob_user:
            return None

        # 已有凭证，直接返回
        if ob_user["sa_username"]:
            sa_user = conn.execute(
                "SELECT username, password, token FROM users WHERE username=?",
                (ob_user["sa_username"],)
            ).fetchone()
            if sa_user:
                return {
                    "username": sa_user["username"],
                    "password": sa_user["password"],
                    "token": sa_user["token"],
                    "already_had": True,
                }

        # 生成新凭证
        username, password = "", ""
        for _ in range(10):
            username, password = generate_credentials()
            if not conn.execute("SELECT id FROM users WHERE username=?", (username,)).fetchone():
                break
        else:
            return None

        # 写入 users 表
        sa_token = generate_token()
        conn.execute(
            "INSERT INTO users (username, password, email, token, token_ts) VALUES (?,?,?,?,?)",
            (username, _hash_pwd(password), ob_user["email"], sa_token, time.time())
        )

        # 关联到 offbyone_users
        conn.execute(
            "UPDATE offbyone_users SET sa_username=? WHERE id=?",
            (username, ob_user["id"])
        )
        conn.commit()
        return {
            "username": username,
            "password": password,
            "token": sa_token,
            "already_had": False,
        }
    finally:
        conn.close()


# ══════════════════════════════════════════
# 原有 SmartAgents 桌面端接口（保持不变）
# ══════════════════════════════════════════

def register_user(phone: str, email: str = "") -> Optional[dict]:
    conn = get_db()
    try:
        existing = conn.execute("SELECT id FROM users WHERE phone=?", (phone,)).fetchone()
        if existing:
            return None
        username, password = "", ""
        for _ in range(10):
            username, password = generate_credentials()
            if not conn.execute("SELECT id FROM users WHERE username=?", (username,)).fetchone():
                break
        else:
            return None
        token = generate_token()
        conn.execute(
            "INSERT INTO users (username, password, phone, token, token_ts) VALUES (?,?,?,?,?)",
            (username, _hash_pwd(password), phone, token, time.time())
        )
        conn.commit()
        return {"username": username, "password": password, "token": token}
    finally:
        conn.close()


def join_waitlist(phone: str, email: str = "") -> dict:
    conn = get_db()
    try:
        existing = conn.execute("SELECT id, queue_number FROM waitlist WHERE phone=?", (phone,)).fetchone()
        if existing:
            return {"queue_number": existing["queue_number"], "already_registered": True}
        max_q = conn.execute("SELECT COALESCE(MAX(queue_number), 0) FROM waitlist").fetchone()[0]
        qn = max_q + 1
        conn.execute(
            "INSERT INTO waitlist (phone, email, queue_number) VALUES (?,?,?)",
            (phone, email, qn)
        )
        conn.commit()
        return {"queue_number": qn, "already_registered": False}
    finally:
        conn.close()


def verify_login(username: str, password: str) -> Optional[str]:
    conn = get_db()
    try:
        user = conn.execute(
            "SELECT token, password, status FROM users WHERE username=?",
            (username,)
        ).fetchone()
        if not user:
            return None
        if user["password"] != _hash_pwd(password):
            return None
        if user["status"] != "active":
            return None
        new_token = generate_token()
        conn.execute(
            "UPDATE users SET token=?, token_ts=?, last_login=? WHERE username=?",
            (new_token, time.time(), time.time(), username)
        )
        conn.commit()
        return new_token
    finally:
        conn.close()


def verify_token(token: str) -> Optional[dict]:
    conn = get_db()
    try:
        user = conn.execute(
            "SELECT username, phone, status, token_ts FROM users WHERE token=?",
            (token,)
        ).fetchone()
        if not user:
            return None
        if user["status"] != "active":
            return None
        if time.time() - user["token_ts"] > 7 * 86400:
            return None
        return {"username": user["username"], "phone": user["phone"]}
    finally:
        conn.close()


def check_rate_limit(key: str, window: float = 60, max_req: int = 10) -> bool:
    conn = get_db()
    try:
        now = time.time()
        conn.execute("DELETE FROM rate_limits WHERE window_start < ?", (now - window,))
        row = conn.execute("SELECT count, window_start FROM rate_limits WHERE key=?", (key,)).fetchone()
        if row and now - row["window_start"] < window:
            if row["count"] >= max_req:
                return False
            conn.execute("UPDATE rate_limits SET count=count+1 WHERE key=?", (key,))
        else:
            conn.execute(
                "INSERT OR REPLACE INTO rate_limits (key, count, window_start) VALUES (?,1,?)",
                (key, now)
            )
        conn.commit()
        return True
    finally:
        conn.close()


def log_login(username: str, ip: str, success: bool):
    conn = get_db()
    try:
        conn.execute(
            "INSERT INTO login_log (username, ip, success) VALUES (?,?,?)",
            (username, ip, int(success))
        )
        conn.commit()
    finally:
        conn.close()


init_db()
