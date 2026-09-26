# TodoApp

Simple Android todo list with deadlines.

## Features

- Add, edit and delete todo items
- Mark items as done (strike-through)
- Set a deadline for each item via a date picker
- Local notification when a deadline is reached
- Undo on delete

## Tech

- Kotlin, single-activity, view binding
- Data stored as JSON in `SharedPreferences`
- Deadline reminders via `AlarmManager` + a `Service` that reschedules the next
  upcoming alarm whenever the list changes or the device reboots

## Build

```
./gradlew assembleDebug
```

Requires JDK 17+ and Android SDK 34.
