package io.github.ibengineering.nnt.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.style.BaseStyles;

import io.github.ibengineering.nnt.AbstractEvolverState;

public class EvolverGuiHudState extends BaseAppState {

	private Node subGuiNode;
	private Container leftButtonPanel;
	
	private AbstractEvolverState aes;
	
	public EvolverGuiHudState(AbstractEvolverState aes) {
		this.aes = aes;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void initialize(Application app) {
		if(GuiGlobals.getInstance() == null) {
			GuiGlobals.initialize(app);
			BaseStyles.loadGlassStyle();
			GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
		}
		
		subGuiNode = new Node();
		((SimpleApplication)app).getGuiNode().attachChild(subGuiNode);
		leftButtonPanel = new Container();
		subGuiNode.attachChild(leftButtonPanel);
		
		int w = app.getCamera().getWidth();
		int h = app.getCamera().getHeight();
		
		leftButtonPanel.setPreferredSize(new Vector3f(240, h, 0));
		leftButtonPanel.setLocalTranslation(0, h, 0);
		leftButtonPanel.setInsets(new Insets3f(24, 24, 24, 24));
		
		Button pauseButton = leftButtonPanel.addChild(new Button("Pause"));
		pauseButton.addClickCommands((Button src) -> {
			aes.setEnabled(!aes.isEnabled());	//toggle
		});
		pauseButton.setIcon(new IconComponent("icons/pause.png"));
		
		Button saveButton = leftButtonPanel.addChild(new Button("Save Generation"));
		saveButton.addClickCommands((Button src) -> {
		});
		saveButton.setIcon(new IconComponent("icons/save.png"));
		
//		Button loadButton = leftButtonPanel.addChild(new Button("Load Generation"));
//		loadButton.addClickCommands((Button src) -> {
//		});
//		loadButton.setIcon(new IconComponent("icons/load.png"));
	}

	@Override
	protected void cleanup(Application app) {
		
	}

	@Override
	protected void onEnable() {
	}

	@Override
	protected void onDisable() {
	}
	
	
	
}
