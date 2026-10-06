FROM gcr.io/distroless/java25-debian13

WORKDIR /app
COPY sigil.jar /app/sigil.jar

EXPOSE 8080
CMD ["/app/sigil.jar"]
