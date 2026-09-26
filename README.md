# TodoApp

Simple Android todo list with deadlines.

## Features

- Add, edit and delete todo items
- Mark items as done (strike-through)
- Set a deadline for each item via a date picker
- Local notification when a deadline is reached
- Undo on delete
- Optional [Kanboard](https://kanboard.org) sync (menu → Kanboard): set your
  server URL, API token and project name, and todos are pushed as Kanboard
  tasks and kept in sync (create/rename/complete/reopen/delete). One-way
  (app → Kanboard) only.

## Tech

- Kotlin, single-activity, view binding
- Data stored as JSON in `SharedPreferences`
- Deadline reminders via `AlarmManager` + a `Service` that reschedules the next
  upcoming alarm whenever the list changes or the device reboots
- Kanboard sync via its JSON-RPC API (`HttpURLConnection` + `org.json`, no
  extra dependencies), authenticated with the app-level API token

## Build

```
./gradlew assembleDebug
```

Requires JDK 17+ and Android SDK 34.
