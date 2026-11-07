# PR media — author handoff

The PR description draft already embeds screenshots from the feature branch and links the video. Commit/push the approved media with the rest of the changes before previewing those repository links.

1. Paste docs/PR_DESCRIPTION.md's description section into your PR editor.
2. Drag **docs/media/catalogue-walkthrough.mp4** into the video section. GitHub uploads it and inserts an attachment URL; keep that URL on its own line for inline playback. Remove the fallback download link if you prefer just the player.
3. Preview the screenshots/video, create the PR into main and leave it open. Private-repo reviewers need repository access. If private-repository image embeds do not load, drag the PNG files into the same editor too and use GitHub’s generated image URLs.

Video: actual Samsung API 36 phone, live Fake Store products through the local backend, product detail → invalid fields → synthetic enquiry → server-confirmed thank-you. The matching Mailpit email was checked separately. The MP4 is H.264, about 1.5 MB and 59 seconds, silent and unsped. It is a phone screen recording rather than a mockup. Failure/restoration coverage remains in the earlier verification notes; the video does not claim to show that separate walkthrough.

Images: docs/screenshots/catalogue.png, product-details.png, enquiry.png, confirmation.png and live-products.png are live-mode phone captures with system bars cropped. retained-form.png is the earlier controlled demo SMTP-failure capture.

A reviewer browsing on a phone can view/download media without local tooling. Running the app requires Android plus a computer hosting the backend, as explained in README. The local backend and inbox links do not point to a public hosted demo.

GitHub's [file-attachment documentation](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/attaching-files) describes uploads and supported video formats. No attachment URL is fabricated, and no PR/upload has been made by the assistant.
