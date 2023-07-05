package me.mathjx.extensiblefood;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

final class ExecutableJar {

	@SuppressWarnings("deprecation")
	public static void main(final String[] args) {
		if (GraphicsEnvironment.isHeadless()) return;

		final String[] options = { "Open Online Wiki Page", /* "Open Intern Wiki Page", */ "Close" };
		final JOptionPane optionPane = new JOptionPane("<html>" + "<h3>About:</h3>"
				+ "<p style=\"text-align: justify;width: 200px;\">A small mod which doesn't add anything directly... But allows you to add your own foods!</p>"
				+ "<h3>Credits:</h3>" + "<ul>"
				+ "<li><strong><em>Blu3_Squirr3l</em></strong>, for the original idea, and tests</li>"
				+ "<li><strong><em>mathjx</em></strong>, for the mod development</li>" + "</ul>"
				+ "</html>", JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, new ImageIcon(ExecutableJar.class.getResource("/assets/extensible_food/icon.png")), options, null);
		final JDialog dialog = optionPane.createDialog("ExtensibleFood");

		dialog.show();
		dialog.dispose();

		final Object selected = optionPane.getValue();

		if (selected == null) {} else if (selected == options[0]) {
			browseOnlineDocPage();
//		} else if (selected == options[1]) {
//			browseInternalDocPage();
		} else if (selected == options[1]) {
			// System.exit(0);
		}
	}

//	private static boolean browseInternalDocPage() {
//		try {
//			if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()) {
//				Desktop desktop = Desktop.getDesktop();
//
//				if (desktop.isSupported(Desktop.Action.BROWSE)) {
//					try (InputStream stream = ExecutableJar.class.getClassLoader().getResourceAsStream("format_documentation.html")) {
//						if (stream != null) {
//							Path file = Files.createTempFile("format_documentation", ".html");
//							Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
//							desktop.browse(file.toUri());
//							return true;
//						}
//					}
//
//					return browseOnlineDocPage();
//				}
//			}
//		} catch (Throwable e) {
//			e.printStackTrace();
//		}
//
//		return false;
//	}

	private static boolean browseOnlineDocPage() {
		try {
			if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()) {
				final Desktop desktop = Desktop.getDesktop();

				if (desktop.isSupported(Desktop.Action.BROWSE)) {
					desktop.browse(new URI("http://htmlpreview.github.io/?https://github.com/mathjx000/ExtensibleFood/blob/main/docs/index.min.html"));
					return true;
				}
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}

		return false;
	}

}
