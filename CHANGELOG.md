# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.1] - 2025-07-22
### Changed
- Updated translations

### Fixed
- Fixed crash on startup ([#247])

## [1.3.0] - 2025-07-13
### Changed
- Updated translations

### Fixed
- It's now possible to reselect the default system alarm sound ([#144])

## [1.2.2] - 2025-06-21
### Changed
- Replaced lap text button with an icon button
- Updated stopwatch layout animation
- Updated translations

### Fixed
- Fixed inaccuracy in stopwatch over long durations ([#207])
- Fixed text jitter in clock, stopwatch, and timer ([#11])
- Fixed invisible stopwatch laps in landscape mode ([#107])
- Fixed overlapping text in right-to-left languages ([#206])

## [1.2.1] - 2025-05-08
### Changed
- Updated translations

### Fixed
- Fixed gradual alarm volume increase on Samsung devices ([#158])

## [1.2.0] - 2025-04-15
### Added
- Added notifications for missed or replaced alarms
- Added placeholders for empty alarms and timers (#124)

### Changed
- Replaced checkboxes with switches (https://github.com/orgs/FossifyOrg/discussions/78)
- Other minor bug fixes and improvements
- Added more translations

### Removed
- Removed support for Android 7 and older
  versions (https://github.com/orgs/FossifyOrg/discussions/241)

### Fixed
- Fixed issue where alarms could be silenced unintentionally (#77, #93)
- Fixed issue where alarms didn't go off in some cases (#89)
- Fixed broken behavior when re-enabling one-time alarms (#110)

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

[#11]: https://github.com/FossifyOrg/Clock/issues/11
[#107]: https://github.com/FossifyOrg/Clock/issues/107
[#144]: https://github.com/FossifyOrg/Clock/issues/144
[#158]: https://github.com/FossifyOrg/Clock/issues/158
[#206]: https://github.com/FossifyOrg/Clock/issues/206
[#207]: https://github.com/FossifyOrg/Clock/issues/207
[#247]: https://github.com/FossifyOrg/Clock/issues/247

[Unreleased]: https://github.com/FossifyOrg/Clock/compare/1.3.1...HEAD
[1.3.1]: https://github.com/FossifyOrg/Clock/compare/1.3.0...1.3.1
[1.3.0]: https://github.com/FossifyOrg/Clock/compare/1.2.2...1.3.0
[1.2.2]: https://github.com/FossifyOrg/Clock/compare/1.2.1...1.2.2
[1.2.1]: https://github.com/FossifyOrg/Clock/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/FossifyOrg/Clock/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/FossifyOrg/Clock/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/FossifyOrg/Clock/releases/tag/1.0.0
