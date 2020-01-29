## Translating for Essentials
To translate this plugin, You need check the list of languages currently translated below.<br>
[Show translated file path](https://github.com/Kieaer/Essentials/tree/master/src/main/resources/bundle)<br>

Korean, English: 100%<br>
Chinese, Russian: 30~40%

If it's not on the list, you'll need to create a file.
Download the [Bundle.properties](https://raw.githubusercontent.com/Kieaer/Essentials/master/src/main/resources/bundle/bundle_en.properties) file to the ``config/mods/Esentials`` folder.

When you run server, the plugin retrieves the files from the external instead of the files internal.

### Useful Information
* When you see text surrounded by square brackets, such as ``[RED]``, ``[]`` or ``[accent]``, this indicates a color code. Don't translate it.
* ``{0}`` means an argument that will be replaced when the text is displayed. For example, Plugin language: ``{0}`` will replace the ``{0}`` with whatever config file you are in.
* ``\n`` means "new line". If you want to split text into multiple lines, use ``\n`` to do it.
