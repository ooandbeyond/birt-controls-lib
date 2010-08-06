/*
 * (C) Copyright Blackboard Inc. 1998-2009 - All Rights Reserved
 *
 * Permission to use, copy, modify, and distribute this software without prior explicit written approval is
 * strictly  prohibited. Please refer to the file "copyright.html" for further important copyright and licensing information.
 *
 * BLACKBOARD MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT.
 * BLACKBOARD SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * authors: Scott Rosenbaum / Steve Schafer, Innovent Solutions, Inc.
 */
package blackboard.birt.extensions.dotbar;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.engine.extension.IBaseResultSet;
import org.eclipse.birt.report.engine.extension.ICubeResultSet;
import org.eclipse.birt.report.engine.extension.IQueryResultSet;
import org.eclipse.birt.report.engine.extension.ReportItemPresentationBase;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;

/**
 * Handles the rendering of the control during report generation. This class is
 * specified in the org.eclipse.birt.report.engine.reportitemPresentation
 * extension.
 * 
 * @author Steve Schafer / Innovent Solutions
 */
public class DotbarPresentationImpl extends ReportItemPresentationBase {
	private DotbarItem dotbarItem;

	@Override
	public void setModelObject(final ExtendedItemHandle modelHandle) {
		try {
			dotbarItem = (DotbarItem) modelHandle.getReportItem();
		} catch (final ExtendedElementException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getOutputType() {
		return OUTPUT_AS_IMAGE;
	}

	@Override
	public Object onRowSets(final IBaseResultSet[] results)
			throws BirtException {
		if (dotbarItem == null)
			return null;

		final DotbarData config = dotbarItem.getConfiguration();
		int value = 0;
		if (results != null && results.length > 0) {
			final IBaseResultSet baseResultSet = results[0];
			if (baseResultSet instanceof IQueryResultSet) {
				final IQueryResultSet queryResultSet = (IQueryResultSet) baseResultSet;
				if (queryResultSet.isBeforeFirst())
					queryResultSet.next();
				final Object object = queryResultSet
						.evaluate(config.valueExpression);
				value = translateObject(object);
			} else if (baseResultSet instanceof ICubeResultSet) {
				final ICubeResultSet cubeResultSet = (ICubeResultSet) baseResultSet;
				final Object object = cubeResultSet
						.evaluate(config.valueExpression);
				value = translateObject(object);
			}
		} else {
			final Object object = context.evaluate(config.valueExpression);
			value = translateObject(object);
		}

		final DotbarData.Calculator calculator = config.new Calculator(1, "in",
				dpi);
		final BufferedImage bufferedImage = calculator.createSwingImage(value,
				dotbarItem.getModelHandle());
		ByteArrayInputStream bis = null;
		try {
			ImageIO.setUseCache(false);
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
			ImageIO.write(bufferedImage, "png", ios); //$NON-NLS-1$
			ios.flush();
			ios.close();
			bis = new ByteArrayInputStream(baos.toByteArray());
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return bis;
	}

	private int translateObject(final Object object) {
		if (object instanceof Integer)
			return ((Integer) object).intValue();
		if (object instanceof Long)
			return ((Long) object).intValue();
		if (object instanceof BigDecimal)
			return ((BigDecimal) object).intValue();
		if (object instanceof Double)
			return ((Double) object).intValue();
		if (object instanceof String)
			try {
				return Integer.parseInt((String) object);
			} catch (final NumberFormatException e) {
				return 0;
			}
		return 0;
	}
}
