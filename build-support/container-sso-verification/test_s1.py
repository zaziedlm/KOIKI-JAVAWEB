"""Five guard tests: refuse foreign resources before any destructive command."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import cleanup
import preflight


class OwnershipTests(unittest.TestCase):
    def test_foreign_work_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            work = Path(temp)
            (work / 'manifest.json').write_text(json.dumps({'work': str(work)}))
            with self.assertRaises(ValueError):
                preflight.load(work)

    def test_existing_project_is_rejected_before_command(self):
        with patch.object(preflight, 'resources', return_value={'containers': ['foreign']}), \
                patch.object(preflight, 'command') as command:
            with self.assertRaises(RuntimeError):
                preflight.preflight(Path('/not-used'), {'project': 'd3-sso-s1-20261109-test'}, [])
            command.assert_not_called()

    def test_cleanup_requires_matching_execution(self):
        with tempfile.TemporaryDirectory() as temp:
            work = Path(temp)
            (work / 'execution.json').write_text(json.dumps({'project': 'foreign', 'started_with_empty_project': True}))
            with patch.object(cleanup, 'command') as command:
                with self.assertRaises(RuntimeError):
                    cleanup.cleanup(work, {'project': 'owned'}, [])
                command.assert_not_called()

    def test_cleanup_refuses_unknown_volume(self):
        with tempfile.TemporaryDirectory() as temp:
            work = Path(temp)
            (work / 'execution.json').write_text(json.dumps({'project': 'owned', 'started_with_empty_project': True}))
            with patch.object(cleanup, 'resources', return_value={'containers': [], 'volumes': ['foreign'], 'networks': []}), \
                    patch.object(cleanup, 'command') as command:
                with self.assertRaises(RuntimeError):
                    cleanup.cleanup(work, {'project': 'owned', 'permitted_resources': {'volume': 'owned_database', 'network': 'owned_database'}}, [])
                command.assert_not_called()

    def test_db_selector_cannot_inject_shell(self):
        with patch.object(preflight, 'command') as command:
            with self.assertRaises(ValueError):
                preflight.sql(Path('/unused'), [], 'koiki_app;unapproved', 'koiki_app', 'SELECT 1')
            command.assert_not_called()


if __name__ == '__main__':
    unittest.main()
