# Usage

Full command reference for `md2pdf`. For a project overview, build
instructions, and architecture notes, see [README.md](README.md).

## Synopsis

```
java -jar md2pdf.jar [<input.md>] [OPTIONS]
```

Running the jar with no arguments prints this same help text and exits with
code `2` (invalid usage) — there is no separate "no input" error message to
remember, the full option list is always one command away:

```bash
java -jar md2pdf.jar
```

## Options

| Option                      | Description                                                                                                                                                   |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `<input.md>` / `-i, --input` | Markdown file to convert. Either a positional argument or `--input` — the positional form is the simplest for everyday use.                                  |
| `-o, --output`               | Destination PDF file. Defaults to the input file name with a `.pdf` extension, in the same directory as the input.                                            |
| `--docx`                     | Also produce a DOCX file alongside the PDF, using the input file name with a `.docx` extension.                                                               |
| `--docx-output`              | Destination DOCX file. Implies `--docx` — you don't need both flags.                                                                                          |
| `--mermaid-mode`              | `image` (default) renders each Mermaid diagram to a picture via `mmdc` and embeds it in the output. `text` leaves the diagram source as a plain fenced code block and never invokes `mmdc`. Case-insensitive (`IMAGE`/`image` both work). |
| `--mmdc`                     | Path to the Mermaid CLI executable. Defaults to `mmdc`, resolved against `PATH`. Only relevant in `image` mode.                                               |
| `--background`               | Background colour for rendered Mermaid diagrams. Defaults to `white` (avoids transparent PNGs looking odd on a white page). Only relevant in `image` mode.    |
| `-h, --help`                 | Show the help message and exit.                                                                                                                                |
| `-V, --version`              | Show version information and exit.                                                                                                                             |

## Exit codes

| Code | Meaning                                                          |
|------|-------------------------------------------------------------------|
| `0`  | Success.                                                           |
| `1`  | Conversion error (bad input file, `mmdc` failure, rendering error, etc). The error message is printed to stderr. |
| `2`  | Invalid usage (no input file supplied, unknown option, etc). Full help is printed. |

## Examples

### Basic PDF conversion

```bash
java -jar md2pdf.jar README.md
# -> README.pdf next to README.md
```

### Explicit input and output paths

```bash
java -jar md2pdf.jar --input docs/guide.md --output out/guide.pdf
```

### Also generate a DOCX

```bash
# Default DOCX name (guide.docx, next to guide.md)
java -jar md2pdf.jar docs/guide.md --docx

# Custom DOCX path (--docx-output implies --docx, no need to pass both)
java -jar md2pdf.jar docs/guide.md --docx-output out/guide.docx

# Both PDF and DOCX with custom paths
java -jar md2pdf.jar docs/guide.md -o out/guide.pdf --docx-output out/guide.docx
```

### Mermaid diagrams as text instead of images

Useful when `mmdc`/Node.js is not installed, or when you want the raw diagram
source visible/editable in the output document instead of a rendered picture:

```bash
java -jar md2pdf.jar guide.md --mermaid-mode text
```

In `text` mode, `mmdc` is never invoked — `--mmdc` and `--background` are
ignored.

### Custom `mmdc` location and diagram background

```bash
java -jar md2pdf.jar guide.md --mmdc /usr/local/bin/mmdc --background transparent
```

On Windows, if `mmdc` was installed globally with npm but isn't found even
though it works when typed directly in a terminal, see
[Troubleshooting](#mmdc-not-found-on-windows-even-though-it-works-in-a-terminal)
below before falling back to `--mmdc` with an explicit path.

## Troubleshooting

### `mmdc` not found on Windows even though it works in a terminal

npm installs global CLIs on Windows as `.cmd`/`.bat` wrapper scripts, not
`.exe` files. When you type `mmdc` in `cmd.exe`, the shell resolves the
extension for you via `PATHEXT`. Java's `ProcessBuilder` calls the Windows
process-creation API directly and does not do this resolution.

`md2pdf` works around this automatically: if `mmdc` (or whatever `--mmdc`
value is configured) has no extension, it searches every directory on `PATH`
for a matching `.cmd`, `.exe`, or `.bat` file and launches that by its
absolute path. If `mmdc` is genuinely not installed or not on `PATH`, you'll
see:

```
Error: Failed to run mmdc. Tried to launch '...'. Is it installed and on the PATH?
```

To fix it:

1. Confirm the install: `npm list -g @mermaid-js/mermaid-cli`.
2. Find where npm put it: `npm config get prefix` (on Windows this is
   typically `%APPDATA%\npm`).
3. Either add that directory to your `PATH` and open a new terminal, or pass
   the full path directly:
   ```bash
   java -jar md2pdf.jar guide.md --mmdc "C:\Users\<you>\AppData\Roaming\npm\mmdc.cmd"
   ```
4. If you don't need rendered diagram images, `--mermaid-mode text` sidesteps
   the whole issue — `mmdc` is never invoked.

### Conversion hangs

`mmdc` runs as a child process with a 2-minute timeout — if it doesn't finish
in time (for example, waiting on Puppeteer/Chromium to launch on a machine
without the required dependencies), the process is killed and conversion
fails with a "timed out" error rather than hanging forever.
