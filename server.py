import http.server
import socketserver
import os
import mimetypes

PORT = 3000
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PUBLIC_DIR = os.path.join(BASE_DIR, "public")

os.makedirs(PUBLIC_DIR, exist_ok=True)

# Register APK mimetype
mimetypes.add_type('application/vnd.android.package-archive', '.apk')

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=PUBLIC_DIR, **kwargs)

    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        if self.path.endswith('.apk'):
            self.send_header('Content-Type', 'application/vnd.android.package-archive')
            self.send_header('Content-Disposition', 'attachment; filename="Movieverse.apk"')
        super().end_headers()

class ReusableServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True

if __name__ == "__main__":
    os.chdir(PUBLIC_DIR)
    print(f"Starting Movieverse download server on port {PORT} from {PUBLIC_DIR}...")
    with ReusableServer(("", PORT), Handler) as httpd:
        httpd.serve_forever()
