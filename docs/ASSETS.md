# Asset attribution

- Instrument Serif is bundled as `android/app/src/main/res/font/instrument_serif.ttf` for display and headline styles. Source: [Google Fonts — Instrument Serif](https://github.com/google/fonts/tree/main/ofl/instrumentserif). Its SIL Open Font License and copyright notice are preserved in `docs/licenses/InstrumentSerif-OFL.txt`.
- DM Sans is bundled as `android/app/src/main/res/font/dm_sans.ttf` for title, body and label styles. Source: [Google Fonts — DM Sans](https://github.com/google/fonts/tree/main/ofl/dmsans). Its SIL Open Font License and copyright notice are preserved in `docs/licenses/DMSans-OFL.txt`.

Both original font binaries and their licence texts are unchanged from the preserved MVP assets. DM Sans contains a variable weight axis; the font family registers regular (400) and medium (500) weights from that same file. Fonts ship with the APK and require no runtime download or font-provider account.


## Product images and fixtures

The thirteen numbered images in backend/public/images/ and the debug preview image were reused from the preserved PoC assets, originally sourced from the [Fake Store API product image repository](https://github.com/keikaavousi/fake-store-api/tree/master/public/img). Product titles/descriptions/prices/ratings in fixtures are illustrative demo content rather than claims of availability or specifications. This records provenance; it does not claim ownership or grant additional rights for commercial reuse. Review/replace photography before a commercial release.

Simple check/star/clear/image-placeholder icons are local Android vector resources; no icon-provider account is required. The app still uses the platform's default launcher icon; a branded launcher icon is a documented polish gap.
