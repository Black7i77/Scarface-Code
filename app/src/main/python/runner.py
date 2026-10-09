"""Execute a single script in the disposable Android runtime process."""
import contextlib
import sys
import traceback

LIMIT = 64 * 1024

class Output:
    def __init__(self, channel, emit, budget):
        self.channel = channel
        self.emit = emit
        self.budget = budget

    def write(self, text):
        text = str(text)
        remaining = max(0, LIMIT - self.budget[0])
        if remaining:
            value = text[:remaining]
            self.budget[0] += len(value)
            # Binder-safe chunks; UTF-16 expands non-BMP characters.
            for start in range(0, len(value), 2048):
                self.emit(self.channel, value[start:start + 2048])
        if len(text) > remaining and not self.budget[1]:
            self.budget[1] = True
            self.emit(self.channel, '\n[output truncated]\n')
        return len(text)

    def flush(self):
        pass

    def isatty(self):
        return False

    @property
    def encoding(self):
        return 'utf-8'


def execute(source, filename, emit, debug=False):
    budget = [0, False]
    out = Output('stdout', emit, budget)
    err = Output('stderr', emit, budget)
    namespace = {'__name__': '__main__', '__file__': filename}
    def trace(frame, event, arg):
        if event != 'line' or frame.f_code.co_filename != filename:
            return trace
        # Bounded diagnostic trace; no pauses or interactive breakpoints yet.
        if trace.steps < 100:
            values = []
            for key, value in list(frame.f_locals.items())[:8]:
                if key.startswith('__'):
                    continue
                try:
                    display = repr(value)
                except BaseException:
                    display = '<unavailable>'
                values.append(f'{key}={display[:80]}')
            emit('stderr', f'[debug] line {frame.f_lineno}: {", ".join(values)}\n')
        elif trace.steps == 100:
            emit('stderr', '[debug] trace limit reached (100 steps)\n')
        trace.steps += 1
        return trace
    trace.steps = 0
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
        try:
            compiled = compile(source, filename, 'exec')
            if debug:
                sys.settrace(trace)
            try:
                exec(compiled, namespace, namespace)
            finally:
                if debug:
                    sys.settrace(None)
            return True
        except BaseException:
            traceback.print_exc()
            return False


def java_emitter(callback):
    """Convert the explicit Java output interface to a Python callable."""
    return lambda channel, text: callback.emit(channel, text)
