import { createEditor } from 'lexical';
import { registerRichText } from '@lexical/rich-text';

const config = {
  namespace: 'Repro',
  onError: (e) => console.error(e),
};

const editor = createEditor(config);
editor.setRootElement(document.getElementById('editor'));
registerRichText(editor);
