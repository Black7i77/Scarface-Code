# Third-party components

| Component | Version | License | Source |
| --- | --- | --- | --- |
| Sora editor and TextMate module | 0.24.4 | LGPL-2.1-or-later | https://github.com/Rosemoe/sora-editor |
| Chaquopy | 17.0.0 | MIT | https://github.com/chaquo/chaquopy |
| CPython | 3.13 bundled build | Python Software Foundation license | https://github.com/python/cpython |
| Rhino | 1.8.0 | MPL-2.0 | https://github.com/mozilla/rhino |
| AndroidX | versions in app/build.gradle.kts | Apache-2.0 | https://android.googlesource.com/platform/frameworks/support/ |
| Gradle wrapper | 8.13 | Apache-2.0 | https://github.com/gradle/gradle |

Sora source archives are supplied in `third-party-sources/`. Sora's TextMate implementation includes Eclipse TM4E (EPL-2.0) and Joni/JCodings (MIT). Dependency POM and bundled metadata retain upstream attributions. The generated TextMate grammars and Scarface theme are original project code; no VS Code logo or proprietary assets are included.

No dependency sources were modified. All application source and build files are supplied, allowing re-linking with modified LGPL editor libraries. See `licenses/` for license texts and upstream source links above for complete attribution.
