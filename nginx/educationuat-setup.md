# educationuat.miraista.com Setup

## Step 1: SSL Certificate

**Option A — Standalone (simplest, nginx stopped briefly):**
```bash
sudo systemctl stop nginx
sudo certbot certonly --standalone -d educationuat.miraista.com
sudo systemctl start nginx
```

**Option B — With nginx running (needs HTTP config first):**
1. Copy `educationuat-http-only.conf` to `/etc/nginx/conf.d/`
2. `sudo nginx -t && sudo systemctl reload nginx`
3. `sudo certbot certonly --nginx -d educationuat.miraista.com`
4. Replace with full `education.conf`

---

## Step 2: Copy nginx config

```bash
sudo cp /home/ec2-user/Ukti/ukti-backend/nginx/education.conf /etc/nginx/conf.d/
```

---

## Step 3: Test and reload nginx

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## Step 4: Deploy Ukti backend

```bash
cd /home/ec2-user/Ukti/ukti-backend
./deploy-ukti.sh
```

---

## Checklist

- [ ] DNS: A record `educationuat.miraista.com` → EC2 public IP
- [ ] Security group: ports 80, 443 inbound
- [ ] RDS: `application-ec2.properties` with correct DB URL
- [ ] CORS: `educationuat.miraista.com` in allowed origins (add if needed)
