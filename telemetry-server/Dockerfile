FROM python:3.12-alpine
RUN adduser -D -u 10001 app && mkdir /data && chown app /data
WORKDIR /srv
COPY server.py .
USER app
VOLUME /data
EXPOSE 8000
CMD ["python", "-u", "server.py"]
