# Contributing

Thanks for your interest in contributing to MixMates Listener for Android.

## Getting started

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Open a pull request

## Development setup

See the [README](README.md) for build instructions. You'll need Android Studio and JDK 17.

## Guidelines

- **Keep changes focused** — one feature or fix per pull request
- **Follow existing patterns** — the project uses MVVM with Hilt DI, Compose UI, and Retrofit networking
- **Test on a device or emulator** before submitting — audio recognition requires microphone access
- **No new dependencies** without discussion — open an issue first to discuss if you'd like to add a library

## Reporting bugs

Open an issue with:
- What you expected to happen
- What actually happened
- Steps to reproduce
- Device and Android version

## Feature requests

Open an issue describing the feature and why it would be useful. Check the [roadmap](docs/plans/) to see if it's already planned.

## Code style

- Kotlin with standard conventions
- Trailing commas in function parameters and collection literals
- Compose functions are `PascalCase`, everything else follows Kotlin conventions
