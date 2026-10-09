# Python debug trace (experimental)

Long-press the existing Run button in the OUTPUT header to run a `.py` file with diagnostic tracing. Normal taps still run code normally. The existing layout and version are unchanged.

The output shows executed source line numbers and a limited snapshot of local variables (first 8 per line, repr limited to 80 characters). The trace is capped at 100 line events. It runs locally using Chaquopy.

**Limitations:** This is a diagnostic trace, not an interactive debugger: there are no breakpoints, pause/resume, step controls or stack UI yet. Debug output may expose values from your script, so avoid sharing it if it contains secrets. Debug tracing adds overhead and still obeys the existing 30-second runtime timeout. JavaScript debugging and new language compilers are not implemented in this stage.
