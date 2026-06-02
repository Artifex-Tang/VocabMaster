# ---- Runtime only (built on host) ----
FROM nginx:1.27-alpine
COPY dist /usr/share/nginx/html
COPY deploy/web-nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
