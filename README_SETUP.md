NAD Remote (T758 V3) — lihtne Android äpp (Compose + WebSocket)

Mis on sees:
- WebSocket juhtimine: ws://<ip>:8585/
- Nupud: Volume +/-, Power toggle, Input selection (1..8)
- Seadme leidmine:
  - mDNS (BluOS): _musc._tcp. (tihti ka _musp._tcp.)
  - Fallback: subnet scan (port 8585)
- Viimase IP meeldejätmine (DataStore)

Kuidas käima saada:
1) Ava Android Studio → Open… ja vali selle zipi lahtipakitud kaust "NadRemote".
2) Kui Android Studio küsib Gradle/AGP uuendust, võid nõustuda.
3) Run telefonis (samas Wi‑Fi võrgus).

Märkus:
- Siin paketis EI ole Gradle wrapper jar-i (gradle/wrapper/gradle-wrapper.jar).
  Android Studio saab tavaliselt projekti siiski importida ning pakub wrapperi taastamist/generatsiooni.
  Kui sul tuleb error "Could not find or load main class org.gradle.wrapper.GradleWrapperMain",
  siis tee Android Studio terminalis:
      gradle wrapper
  või loo uus "Empty Compose Activity" projekt ja kopeeri siit /app/src ja gradle failid üle.

Kontroll:
- Veendu, et telefon ja NAD on samas subnetis.
- Kui mDNS ei leia, kasuta "Scan subnet" või sisesta IP käsitsi.

Windows build stabiilsus:
- Kui corporate poliitikad teevad `%USERPROFILE%\.gradle` kaustaga probleeme voi `JAVA_HOME` ei lahe alati kaima,
  kasuta projekti juures:
      gradlew-safe.bat :app:assembleDebug
  See kasutab Android Studio JBR-i ja lokaalselt projekti `.gradle-user-home` cache'i.

Privacy policy (Play Console):
- A ready page is included at `docs/privacy-policy.html`.
- Publish it with GitHub Pages (Settings -> Pages -> Deploy from branch -> `main` + `/docs`).
- Then set `PRIVACY_POLICY_URL` in `gradle.properties` to:
    https://<your-github-username>.github.io/NADT758Remote/privacy-policy.html
- Rebuild the app so the in-app Settings link uses the same URL.
