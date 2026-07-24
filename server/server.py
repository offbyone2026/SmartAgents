"""SmartAgents 后端服务 — FastAPI + SQLite + OffByOne 账号体系 + 邮箱验证码"""
import os
import time
import re
import sys
import smtplib
from email.mime.text import MIMEText
from email.header import Header
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from database import (
    register_user, join_waitlist, verify_login, verify_token,
    check_rate_limit, log_login, init_db,
    store_verification_code, verify_code,
    create_offbyone_user, login_offbyone_user,
    verify_offbyone_token, apply_sa_credentials,
)

from fastapi import FastAPI, Request, HTTPException, Form, Header as FastAPIHeader
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, HTMLResponse, FileResponse
from pydantic import BaseModel
from typing import Optional

# ── 数据库初始化 ──
init_db()

# ── SMTP 配置 ──
SMTP_HOST = os.getenv("SMTP_HOST", "smtp.qq.com")
SMTP_PORT = int(os.getenv("SMTP_PORT", "587"))
SMTP_USER = os.getenv("SMTP_USER", "")
SMTP_PASS = os.getenv("SMTP_PASS", "")
SMTP_FROM_NAME = os.getenv("SMTP_FROM_NAME", "OffByOne")

def send_verification_email(to_email: str, code: str) -> bool:
    """通过 SMTP 发送验证码邮件。返回 True/False"""
    if not SMTP_USER or not SMTP_PASS:
        print(f"[EMAIL] SMTP 未配置，验证码已输出到日志: {to_email} -> {code}")
        return True  # 开发模式：未配置 SMTP 时日志输出，当作发送成功

    try:
        msg = MIMEText(
            f"<div style='font-family:sans-serif;max-width:480px;margin:0 auto;padding:40px 20px;'>"
            f"<h2 style='color:#333;'>OffByOne 邮箱验证</h2>"
            f"<p style='font-size:16px;color:#555;'>您的验证码是：</p>"
            f"<div style='background:#f5f5f5;border-radius:8px;padding:20px;text-align:center;margin:20px 0;'>"
            f"<span style='font-size:36px;font-weight:700;letter-spacing:8px;color:#E53935;'>{code}</span>"
            f"</div>"
            f"<p style='font-size:13px;color:#999;'>验证码 5 分钟内有效，请勿转发给他人。</p>"
            f"<p style='font-size:13px;color:#999;'>如果不是您本人操作，请忽略此邮件。</p>"
            f"</div>",
            "html", "utf-8"
        )
        msg["Subject"] = Header(f"OffByOne 验证码：{code}", "utf-8")
        msg["From"] = f"{SMTP_FROM_NAME} <{SMTP_USER}>"
        msg["To"] = to_email

        server = smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=10)
        server.starttls()
        server.login(SMTP_USER, SMTP_PASS)
        server.sendmail(SMTP_USER, [to_email], msg.as_string())
        server.quit()
        print(f"[EMAIL] 验证码已发送: {to_email} -> {code}")
        return True
    except Exception as e:
        print(f"[EMAIL] 发送失败: {e}")
        return False


# ── FastAPI 应用 ──
app = FastAPI(title="SmartAgents API", version="2.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ══════════════════════════════════════════
# 模型
# ══════════════════════════════════════════
class LoginRequest(BaseModel):
    username: str
    password: str

class VerifyRequest(BaseModel):
    token: str

class SendCodeRequest(BaseModel):
    email: str

class OffByOneRegisterRequest(BaseModel):
    email: str
    code: str
    password: str

class OffByOneLoginRequest(BaseModel):
    email: str
    password: str


# ══════════════════════════════════════════
# 限流常量
# ══════════════════════════════════════════
GLOBAL_WINDOW = 60
GLOBAL_MAX_RPM = 60
LOGIN_MAX_PER_MIN = 10
SEND_CODE_MAX_PER_HOUR = 5   # 每邮箱每小时最多 5 次验证码


# ══════════════════════════════════════════
# 中间件
# ══════════════════════════════════════════

@app.middleware("http")
async def rate_limit_middleware(request: Request, call_next):
    client_ip = request.client.host if request.client else "unknown"
    if not check_rate_limit(f"global:{client_ip}", window=GLOBAL_WINDOW, max_req=GLOBAL_MAX_RPM):
        return JSONResponse(status_code=429, content={"error": "请求过于频繁，请稍后再试"})
    response = await call_next(request)
    return response


@app.middleware("http")
async def honeypot_middleware(request: Request, call_next):
    if request.headers.get("x-hp") or request.query_params.get("hp"):
        return JSONResponse(status_code=403, content={"error": "Forbidden"})
    response = await call_next(request)
    return response


# ══════════════════════════════════════════
# API — 健康检查
# ══════════════════════════════════════════

@app.get("/health")
async def health():
    return {"status": "ok", "version": "2.0.0"}


# ══════════════════════════════════════════
# API — OffByOne 邮箱验证码
# ══════════════════════════════════════════

@app.post("/offbyone/send-code")
async def offbyone_send_code(request: Request, body: SendCodeRequest):
    """发送邮箱验证码：5 次/小时/邮箱"""
    email = body.email.strip().lower()
    if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
        raise HTTPException(status_code=400, detail="邮箱格式不正确")

    client_ip = request.client.host if request.client else ""
    if not check_rate_limit(f"sendcode:{client_ip}", window=3600, max_req=SEND_CODE_MAX_PER_HOUR):
        raise HTTPException(status_code=429, detail="验证码发送次数过多，请 1 小时后再试")

    code = store_verification_code(email)
    success = send_verification_email(email, code)
    if not success:
        raise HTTPException(status_code=500, detail="邮件发送失败，请稍后再试")

    return JSONResponse(content={"success": True, "message": "验证码已发送"})


# ══════════════════════════════════════════
# API — OffByOne 注册
# ══════════════════════════════════════════

@app.post("/offbyone/register")
async def offbyone_register(request: Request, body: OffByOneRegisterRequest):
    """OffByOne 注册：验证码 + 邮箱 + 密码"""
    email = body.email.strip().lower()
    code = body.code.strip()
    password = body.password.strip()

    if not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', email):
        raise HTTPException(status_code=400, detail="邮箱格式不正确")

    if len(password) < 6 or len(password) > 32:
        raise HTTPException(status_code=400, detail="密码长度 6-32 位")

    if not verify_code(email, code):
        raise HTTPException(status_code=400, detail="验证码错误或已过期")

    result = create_offbyone_user(email, password)
    if result is None:
        raise HTTPException(status_code=409, detail="该邮箱已注册")

    return JSONResponse(content={
        "success": True,
        "email": result["email"],
        "token": result["token"],
    })


# ══════════════════════════════════════════
# API — OffByOne 登录
# ══════════════════════════════════════════

@app.post("/offbyone/login")
async def offbyone_login(request: Request, body: OffByOneLoginRequest):
    email = body.email.strip().lower()
    password = body.password.strip()

    client_ip = request.client.host if request.client else ""
    if not check_rate_limit(f"login_ob:{client_ip}", window=60, max_req=LOGIN_MAX_PER_MIN):
        raise HTTPException(status_code=429, detail="登录尝试过于频繁，请 1 分钟后再试")

    result = login_offbyone_user(email, password)
    if result is None:
        raise HTTPException(status_code=401, detail="邮箱或密码错误")

    return JSONResponse(content={
        "success": True,
        "email": result["email"],
        "token": result["token"],
        "has_sa_credentials": result["has_sa_credentials"],
        "sa_username": result["sa_username"],
    })


# ══════════════════════════════════════════
# API — 申请 SmartAgents 内测凭证
# ══════════════════════════════════════════

@app.post("/offbyone/apply-beta")
async def offbyone_apply_beta(authorization: str = FastAPIHeader(...)):
    """申请 SmartAgents 内测凭证（需携带 OffByOne token）"""
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="未授权")
    token = authorization[7:]

    result = apply_sa_credentials(token)
    if result is None:
        raise HTTPException(status_code=401, detail="Token 无效，请重新登录")

    return JSONResponse(content={
        "success": True,
        "username": result["username"],
        "password": result["password"],
        "token": result["token"],
        "already_had": result["already_had"],
    })


# ══════════════════════════════════════════
# API — 原有 SmartAgents 桌面端接口
# ══════════════════════════════════════════

@app.post("/login")
async def login(request: Request, body: LoginRequest):
    client_ip = request.client.host if request.client else ""
    if not check_rate_limit(f"login:{client_ip}", window=60, max_req=LOGIN_MAX_PER_MIN):
        raise HTTPException(status_code=429, detail="登录尝试过于频繁，请 1 分钟后再试")
    token = verify_login(body.username, body.password)
    if token is None:
        log_login(body.username, client_ip, False)
        raise HTTPException(status_code=401, detail="用户名或密码错误")
    log_login(body.username, client_ip, True)
    return JSONResponse(content={"success": True, "token": token})


@app.post("/verify")
async def verify(request: Request, body: VerifyRequest):
    token = verify_token(body.token)
    if token is None:
        raise HTTPException(status_code=401, detail="Token 无效或已过期")
    return JSONResponse(content={"success": True, "username": token["username"]})


@app.post("/register")
async def register(request: Request, phone: str = Form(...), email: str = Form("")):
    client_ip = request.client.host if request.client else ""
    if not check_rate_limit(f"register:{client_ip}", window=3600, max_req=3):
        raise HTTPException(status_code=429, detail="该 IP 注册次数已达上限")
    phone = phone.strip()
    if not re.match(r'^1[3-9]\d{9}$', phone):
        raise HTTPException(status_code=400, detail="手机号格式不正确")
    if not check_rate_limit(f"phone:{phone}", window=300, max_req=1):
        raise HTTPException(status_code=429, detail="该手机号已提交过注册")
    result = register_user(phone, email)
    if result is None:
        raise HTTPException(status_code=409, detail="该手机号已注册或系统繁忙")
    return JSONResponse(content={
        "success": True,
        "username": result["username"],
        "password": result["password"],
        "token": result["token"],
    })


@app.post("/waitlist")
async def waitlist(request: Request, phone: str = Form(...), email: str = Form("")):
    client_ip = request.client.host if request.client else ""
    if not check_rate_limit(f"waitlist:{client_ip}", window=300, max_req=3):
        raise HTTPException(status_code=429, detail="操作过于频繁")
    phone = phone.strip()
    if not re.match(r'^1[3-9]\d{9}$', phone):
        raise HTTPException(status_code=400, detail="手机号格式不正确")
    result = join_waitlist(phone, email)
    return JSONResponse(content={
        "success": True,
        "queue_number": result["queue_number"],
        "already_registered": result["already_registered"],
    })


# ══════════════════════════════════════════
# 静态文件
# ══════════════════════════════════════════
STATIC_DIR = Path(__file__).parent.parent  # output/

@app.get("/", response_class=HTMLResponse)
async def index():
    html_path = STATIC_DIR / "index.html"
    if html_path.exists():
        return html_path.read_text(encoding="utf-8")
    return HTMLResponse("<h1>SmartAgents</h1>")


@app.get("/signup", response_class=HTMLResponse)
async def signup_page():
    """OffByOne 注册/登录页面"""
    html_path = STATIC_DIR / "signup.html"
    if html_path.exists():
        return html_path.read_text(encoding="utf-8")
    return HTMLResponse("<h1>OffByOne Sign Up</h1>")


@app.get("/favicon.ico")
async def favicon():
    return FileResponse(STATIC_DIR / "favicon.ico") if (STATIC_DIR / "favicon.ico").exists() else JSONResponse(status_code=404)


# ══════════════════════════════════════════
# 启动
# ══════════════════════════════════════════
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=3002)
