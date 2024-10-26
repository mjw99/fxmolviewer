package org.openmolecules.fx.sunflow;

import org.sunflow.core.ShadingState;
import org.sunflow.core.shader.GlassShader;
import org.sunflow.image.Color;

/**
 * Created by thomas on 19.04.16.
 */
public class MyGlassShader extends GlassShader {
	private ColorProvider mColorProvider;

	public void setColorProvider(ColorProvider cp) {
		mColorProvider = cp;
	}


}
