package com.github.i49.spine.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

import com.github.i49.spine.api.Epub;
import com.github.i49.spine.api.Publication;
import com.github.i49.spine.api.PublicationWriter;

/**
 * Publication generator that generates a publication from a set of sources.
 */
public class PublicationGenerator {
	
	private Order order = Order.ASCENDING;
	
	public void setDocumentOrder(Order order) {
		this.order = order;
	}
	
	public void generate(Path sourceDir, Path target) throws Exception {
		if (!Files.isDirectory(sourceDir)) {
			throw new NotDirectoryException(sourceDir.toString());
		}
		if (target == null) {
			target = getDefaultTargetPath(sourceDir);
		}

		Publication publication = buildPublication(sourceDir);
		writePublication(target,publication);
	}
	
	private Publication buildPublication(Path sourceDir) throws IOException {
		PublicationBuilder builder = new PublicationBuilder(sourceDir);
		builder.setDocumentOrder(this.order);
		return builder.build();
	}

	private void writePublication(Path target, Publication publication) throws Exception {
		try (PublicationWriter writer = Epub.createWriter(Files.newOutputStream(target))) {
			writer.write(publication);
		}
	}

	private static Path getDefaultTargetPath(Path sourceDir) {
		String filename = sourceDir.getFileName().toString();
		Path parent = sourceDir.toAbsolutePath().getParent();
		return parent.resolve(filename + ".epub");
	}
}
