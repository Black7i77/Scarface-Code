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


def execute(source, filename, emit):
    budget = [0, False]
    out = Output('stdout', emit, budget)
    err = Output('stderr', emit, budget)
    namespace = {'__name__': '__main__', '__file__': filename}
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
        try:
            exec(compile(source, filename, 'exec'), namespace, namespace)
            return True
        except BaseException:
            traceback.print_exc()
            return False


def java_emitter(callback):
    """Convert the explicit Java output interface to a Python callable."""
    return lambda channel, text: callback.emit(channel, text)
