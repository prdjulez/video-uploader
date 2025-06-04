import os
import sys
import pickle
from datetime import datetime
import google_auth_oauthlib.flow
import googleapiclient.discovery
from googleapiclient.http import MediaFileUpload

CLIENT_SECRETS_FILE = "client_secret.json"
SCOPES = ["https://www.googleapis.com/auth/youtube.upload"]
API_SERVICE_NAME = "youtube"
API_VERSION = "v3"

def get_authenticated_service():
    credentials = None
    if os.path.exists("token.pickle"):
        with open("token.pickle", "rb") as token:
            credentials = pickle.load(token)
    if not credentials or not credentials.valid:
        flow = google_auth_oauthlib.flow.InstalledAppFlow.from_client_secrets_file(
            CLIENT_SECRETS_FILE, SCOPES
        )
        try:
            # Lokaler Rechner mit Browser (z. B. VS Code)
            credentials = flow.run_local_server(port=8765)
        except:
            # Server ohne Browser (z. B. Linux Rootserver)
            credentials = flow.run_console()
        with open("token.pickle", "wb") as token:
            pickle.dump(credentials, token)
    return googleapiclient.discovery.build(API_SERVICE_NAME, API_VERSION, credentials=credentials)

def upload_video(file_path, title, description, tags=None, category_id="22", privacy_status="private", publish_time=None):
    if tags is None:
        tags = []

    service = get_authenticated_service()
    body = {
        'snippet': {
            'title': title,
            'description': description,
            'tags': tags,
            'categoryId': category_id
        },
        'status': {
            'privacyStatus': privacy_status,
            'selfDeclaredMadeForKids': False
        }
    }

    if publish_time:
        body['status']['publishAt'] = publish_time
        body['status']['privacyStatus'] = 'private'

    media = MediaFileUpload(file_path, chunksize=-1, resumable=True, mimetype='video/*')
    request = service.videos().insert(part="snippet,status", body=body, media_body=media)

    response = None
    while response is None:
        status, response = request.next_chunk()
        if status:
            print(f"Upload-Fortschritt: {int(status.progress() * 100)}%")
    print("✅ Upload abgeschlossen! Video-ID:", response['id'])

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("❌ Aufruf: python upload_video.py <videopfad> <titel> <beschreibung> [privacy] [releasezeit]")
        print("🔁 Beispiel: python upload_video.py output.mp4 \"Titel\" \"Beschreibung\" private 2025-06-04T20:15")
        sys.exit(1)

    file_path = sys.argv[1]
    title = sys.argv[2]
    description = sys.argv[3]
    privacy = sys.argv[4] if len(sys.argv) > 4 else "private"
    publish_at = sys.argv[5] if len(sys.argv) > 5 and sys.argv[5] else None

    if publish_at:
        publish_at = datetime.fromisoformat(publish_at).isoformat() + "Z"

    upload_video(file_path, title, description, ["website", "auto"], privacy_status=privacy, publish_time=publish_at)
