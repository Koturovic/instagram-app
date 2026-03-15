# ============================================================
# STAGE 1: Build Stage - Kompajlira React + Vite
# ============================================================
FROM node:22-alpine AS build

# Postavlja radni direktorijum
WORKDIR /app

# Kopira package.json i package-lock.json
COPY package*.json ./

# Instalira zavisnosti
RUN npm ci

# Kopira source kod
COPY . .

# Kompajlira React aplikaciju sa Vite
# Rezultat je dist/ direktorijum sa statickim fajlovima
RUN npm run build

# ============================================================
# STAGE 2: Runtime Stage - Služi static fajlove sa Nginx
# ============================================================
FROM nginx:alpine

# Kopira nginx konfiguraciju
COPY nginx.conf /etc/nginx/nginx.conf

# Kopira kompajlirane fajlove iz build stage-a u nginx public direktorijum
COPY --from=build /app/dist /usr/share/nginx/html

# Expozira port 80 (Nginx)
EXPOSE 80

# Pokreće Nginx
CMD ["nginx", "-g", "daemon off;"]

# ============================================================
# Objašnjenje:
# ============================================================
# 1. STAGE 1 (Build):
#    - Koristi Node 22 za kompajliranje
#    - npm ci (clean install) - za reproducibilne buildove
#    - npm run build → generiše dist/ direktorijum
#
# 2. STAGE 2 (Runtime):
#    - Koristi Nginx za serviranje static fajlova
#    - Kopira nginx.conf (trebam da kreiram)
#    - Kopira dist/ fajlove u Nginx public dir
#    - Port 80 je standardni HTTP port
#
# 3. Prednosti Multi-Stage Build-a:
#    - Final image je manja (bez Node, build alata)
#    - Brže pokretanje (samo Nginx)
#    - Bezbijednije (nema source koda u production)

