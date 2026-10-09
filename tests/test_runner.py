import importlib.util
from pathlib import Path
import unittest

path = Path(__file__).parents[1] / 'app/src/main/python/runner.py'
spec = importlib.util.spec_from_file_location('runner', path)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)

class RunnerTests(unittest.TestCase):
    def run_code(self, source):
        output = []
        result = runner.execute(source, 'sample.py', lambda channel, text: output.append((channel, text)))
        return result, ''.join(t for c, t in output if c == 'stdout'), ''.join(t for c, t in output if c == 'stderr')

    def test_print_and_unicode(self):
        ok, out, err = self.run_code("print('Hello 🐺')")
        self.assertTrue(ok); self.assertEqual(out, 'Hello 🐺\n'); self.assertEqual(err, '')

    def test_stderr(self):
        ok, out, err = self.run_code("import sys; print('oops', file=sys.stderr)")
        self.assertTrue(ok); self.assertEqual(err, 'oops\n')

    def test_runtime_error(self):
        ok, out, err = self.run_code('1 / 0')
        self.assertFalse(ok); self.assertIn('ZeroDivisionError', err); self.assertIn('sample.py', err)

    def test_syntax_error(self):
        ok, out, err = self.run_code('if :')
        self.assertFalse(ok); self.assertIn('SyntaxError', err)

    def test_fresh_globals(self):
        self.run_code('secret = 1')
        ok, out, err = self.run_code('print(secret)')
        self.assertFalse(ok); self.assertIn('NameError', err)

    def test_system_exit_is_reported(self):
        ok, out, err = self.run_code('raise SystemExit(5)')
        self.assertFalse(ok); self.assertIn('SystemExit', err)

    def test_output_is_bounded(self):
        ok, out, err = self.run_code("print('x' * 1000000)")
        self.assertTrue(ok); self.assertLessEqual(len(out), 66000); self.assertIn('truncated', out)

if __name__ == '__main__': unittest.main()
