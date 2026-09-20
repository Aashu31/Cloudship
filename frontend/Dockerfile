# ==============================================================================
# CloudShip Frontend — Production Static Web Server
# ==============================================================================
FROM nginx:alpine

# Remove default static content
RUN rm -rf /usr/share/nginx/html/*

# Copy CloudShip frontend static distribution
COPY index.html /usr/share/nginx/html/
COPY css/ /usr/share/nginx/html/css/
COPY js/ /usr/share/nginx/html/js/
COPY img/ /usr/share/nginx/html/img/
COPY cloudship-master-reference.png /usr/share/nginx/html/

# Copy custom Nginx reverse proxy configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
