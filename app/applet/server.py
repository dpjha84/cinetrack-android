import http.server
import socketserver
import os

PORT = 3000
DIRECTORY = "/app/applet/public"

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        super().end_headers()

if __name__ == "__main__":
    os.chdir(DIRECTORY)
    with socketserver.ThreadingTCPServer(("", PORT), Handler) as httpd:
        print(f"Serving {DIRECTORY} on port {PORT}")
        httpd.serve_forever()
