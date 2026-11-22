# Upload Nodes-Extra v1.1.0 to Auroria-Productions/Nodes-Extra

This is the source-only, GitHub-ready version of the 2026-11-21 Nodes-Extra v1.1.0 build.

## Authorisation blocker

The ChatGPT GitHub connection currently belongs to **Milzzyy** and is installed only
on the **Milzzyy personal account**, not the **Auroria-Productions** organisation.
The `Auroria-Productions/Nodes-Extra` repository returned GitHub 404 through that
connection (private/unavailable/permissions cannot be distinguished). No commit was made.

To authorise the upload, open GitHub > Settings > Applications > Installed GitHub Apps,
configure the ChatGPT/GitHub integration for **Auroria-Productions**, and give it access
to the `Nodes-Extra` repository. An organisation owner may need to approve installation.
Then ask ChatGPT to upload the prepared project.

## Manual upload alternative

1. Visit https://github.com/Auroria-Productions/Nodes-Extra
2. Choose **Add file > Upload files** on the default branch, or clone the repo.
3. Upload the **contents of this archive**, not the ZIP as a single file.
4. Commit with message: `Add Nodes-Extra v1.1.0 logistics and government source`.

## Build dependencies

`build.sh` requires Paper API, MilzzyNodes and Java 21, plus the other runtime compile
libraries documented in README.md. They are intentionally NOT included in this source
archive. Download/resolve them locally and set `DEPS_DIR` and `NODES_JAR` before building.
`Nodes-Extra.jar` from the release is a compiled file, not part of this source archive.
