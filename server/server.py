import http.server
import socketserver

ADDRESS = ("192.168.1.149", 8000)


class Handler(http.server.BaseHTTPRequestHandler):
    def _respond(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()

    def do_GET(self):
        if self.path == "/test":
            print("GET-request for test successful")
        self._respond()

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        print("POST:", content_length, post_data)

        self._respond()


with socketserver.TCPServer(ADDRESS, Handler) as server:
    print("Running")
    server.serve_forever()
