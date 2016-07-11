package com.konifar.vectalign;

import com.bonnyfone.vectalign.viewer.VectAlignViewer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class ShowVectalignViewerAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        new VectalignDialog(e.getProject()).show();
    }
}
