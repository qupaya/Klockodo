# Klockodo

This is a simple Linux client for the [Clockodo](https://clockodo.com/) time tracking service that runs in the
statusbar. It is written in Kotlin/Native and uses Gtk3 as GUI toolkit.

## Installation

### 1. Requirements

The following libraries and their development files need to be installed on your computer to build the project:

* Gtk3
* Glib 2.0
* Ayatana AppIndicator3
* Pango
* Cairo
* Harfbuzz
* Atk
* Gdk Pixbuf 2.0

See `src/nativeInterop/cinterop/klockodo.def` for details.

### 2. Building the project

Run

```bash
./gradlew build
```

### 3. Copy the files to the desired location

Currently, things are hardcoded to look for the resource files in your home directory.

#### Configuration

You need to create the following configuration file for Klockodo: `~/.config/Klockodo/config.json`.

The contents of the file are as follows:

```json
{
  "apiKey": "YOUR_API_KEY",
  "apiUser": "YOUR_EMAIL_ADDRESS",
  "defaultProject": DEFAULT_PROJECT_ID,
  "workTimePerDay": "HOURS YOU WORK PER DAY ACCORDING TO CONTRACT IN ISO-8601-2 FORMAT, E.G., PT8H FOR 8 HOURS"
}
```

#### Icons

Copy the PNG files from the `src/main/resources` directory to the directory `~/.local/share/Klockodo/`.

#### Binary

Copy the file `build/bin/native/releaseExecutable/Klockodo.kexe` to the directory `~/.local/bin/`.

#### Desktop file

Depending on whether you want Klockodo to be available in the application menu or autostart on login, copy the file
`src/main/resources/Klockodo.desktop` to the directories:

`~/.local/share/applications/`

and / or

`~/.config/autostart/`
