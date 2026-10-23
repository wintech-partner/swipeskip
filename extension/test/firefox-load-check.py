# Loads an unpacked extension as a temporary add-on in headless Firefox via geckodriver (works with snap Firefox when the path is under ~/snap/firefox/common).
# Usage: python3 firefox-load-check.py /abs/path/to/stage-firefox
import json,subprocess,time,urllib.request,sys
p=subprocess.Popen(["/snap/bin/geckodriver","--port","4455"],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
time.sleep(3)
def r(m,u,b=None):
    q=urllib.request.Request("http://127.0.0.1:4455"+u,data=json.dumps(b).encode() if b is not None else None,method=m,headers={"Content-Type":"application/json"})
    try: return json.load(urllib.request.urlopen(q,timeout=120))
    except urllib.error.HTTPError as e: return json.load(e)
try:
    s=r("POST","/session",{"capabilities":{"alwaysMatch":{"browserName":"firefox","moz:firefoxOptions":{"args":["-headless"]}}}})
    print(json.dumps(s)[:300]); sid=s["value"]["sessionId"]
    print(r("POST",f"/session/{sid}/moz/addon/install",{"path":sys.argv[1],"temporary":True}))
    print(r("POST",f"/session/{sid}/url",{"url":"about:debugging#/runtime/this-firefox"}))
    r("DELETE","/session/"+sid)
finally: p.terminate()
