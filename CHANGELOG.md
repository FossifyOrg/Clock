# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.0] - 2025-04-15

### Added
- Added notifications for missed or replaced alarms
- Added placeholders for empty alarms and timers (#124)

### Changed
- Replaced checkboxes with switches (https://github.com/orgs/FossifyOrg/discussions/78)
- Other minor bug fixes and improvements
- Added more translations

### Fixed
- Fixed issue where alarms could be silenced unintentionally (#77, #93)
- Fixed issue where alarms didn't go off in some cases (#89)
- Fixed broken behavior when re-enabling one-time alarms (#110)

### Removed
- Removed support for Android 7 and older versions (https://github.com/orgs/FossifyOrg/discussions/241)

## [1.1.0] - 2025-03-24

### Added
- Added option to import/export alarms and timers (#105)
- Added option to choose between 12-hour and 24-hour time format (#52)
- Added option to choose first day of week (#19)
- Added option to choose default tab (#5)

### Changed
- Improved sorting options for alarms and timers (#7, #8)
- Other minor fixes and improvements
- Added more translations

### Fixed
- Fixed some issues with alarms not going off (#89, #113)
- Fixed delayed/early alarms due to daylight time saving (#61)
- Fixed issue with snooze button in landscape mode (#85)

## [1.0.0] - 2024-03-24

### Added
- Initial release.

[Unreleased]: https://github.com/FossifyOrg/Clock/compare/1.2.0...HEAD
[1.2.0]: https://github.com/FossifyOrg/Clock/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/FossifyOrg/Clock/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/FossifyOrg/Clock/releases/tag/1.0.0
