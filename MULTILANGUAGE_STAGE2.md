# Scarface Code — Multi-language Stage 2

Preserves the existing Android layout, app icon, package ID, versionCode and versionName.

Added: offline SQLite SQL execution in a temporary in-memory database; JSON syntax validation; XML syntax validation with external entities disabled. These work through the existing Run button and output panel. Python and JavaScript runners are unchanged.

SQL limitations: basic statement splitting (not a complete SQL parser), no SQLite triggers with internal semicolons, maximum 100 statements and 100 displayed rows per query. Database is discarded after each Run. Only use trusted scripts.

Not included: Ruby, Lua, PHP, Java, C/C++, Rust, etc. These require separately bundled Android-compatible runtimes/toolchains. No claims of support are made for them.

Build on Kali: cd project; set JAVA_HOME to JDK 21; set sdk.dir in local.properties; ./gradlew assembleDebug.
