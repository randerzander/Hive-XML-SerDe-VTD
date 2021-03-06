/**
* Hive-XML-SerDe-VTD: VTD Processor for Apache Hive XML SerDe 
*
* Copyright (C) 2013 Dmitry Vasilenko
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package com.ximpleware.hive.serde2.xml.vtd;

import java.util.Deque;
import java.util.LinkedList;

import com.ibm.spss.hive.serde2.xml.processor.XmlNode;
import com.ibm.spss.hive.serde2.xml.processor.XmlTransformer;
import com.ximpleware.AutoPilot;
import com.ximpleware.VTDNav;

/**
 * Defines a VTD XML node
 */
public class VtdNode extends XmlNode {

    private static final String ALL = "*";
    private int depth = 0;

    /**
     * Protected constructor
     */
    protected VtdNode() {
    }

    /**
     * Conversion constructor
     * 
     * @param pilot
     */
    public VtdNode(VtdPilot pilot) {
        try {
            VTDNav navigator = pilot.getVTDNav();
            int currentIndex = navigator.getCurrentIndex();
            switch (navigator.getTokenType(currentIndex)) {
                case VTDNav.TOKEN_ATTR_NAME: {
                    this.type = ATTRIBUTE_NODE;
                    this.name = navigator.toString(currentIndex);
                    this.value = navigator.toString(currentIndex + 1);
                    this.valid = true;
                }
                    break;
                case VTDNav.TOKEN_CHARACTER_DATA: {
                    this.type = TEXT_NODE;
                    String temp = navigator.toString(currentIndex);
                    this.value = temp == null ? null : temp.trim();
                    this.valid = this.value != null && this.value.length() > 0;
                }
                    break;
                case VTDNav.TOKEN_STARTING_TAG: {
                    traverse(pilot, new LinkedList<VtdNode>());
                    // StringBuilder builder = new StringBuilder();
                    // XmlTransformer transformer = new XmlTransformer();
                    // transformer.transform(this, builder);
                    // System.out.println(builder.toString());
                }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void traverse(VtdPilot pilot, Deque<VtdNode> stack) throws Exception {
        VTDNav navigator = pilot.getVTDNav();
        int currentIndex = navigator.getCurrentIndex();
        // int depth = navigator.getCurrentDepth();
        String currentName = navigator.toString(currentIndex);
        // System.out.println(depth + "-------->" + currentName);
        if (stack.size() == 0) {
            this.name = currentName;
            this.type = ELEMENT_NODE;
            this.valid = true;
            addAttributes(this, navigator);
            addText(this, navigator);
            stack.push(this);
        } else {
            switch (navigator.getTokenType(currentIndex)) {
                case VTDNav.TOKEN_STARTING_TAG: {
                    VtdNode node = new VtdNode();
                    node.type = ELEMENT_NODE;
                    node.name = currentName;
                    node.valid = true;
                    addAttributes(node, navigator);
                    addText(node, navigator);
                    stack.peek().addChild(node);
                    stack.push(node);
                }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
        if (navigator.toElement(VTDNav.FC)) {
            do {
                traverse(pilot, stack);
                // currentIndex = navigator.getCurrentIndex();
                // currentName = navigator.toString(currentIndex);
                // System.out.println(currentName);
                // depth = navigator.getCurrentDepth();
                // System.out.println(depth);
            } while (navigator.toElement(VTDNav.NS));
            navigator.toElement(VTDNav.P);
            // if (stack.size() > 0) {
            // stack.pop();
            // }
            currentIndex = navigator.getCurrentIndex();
            depth = navigator.getCurrentDepth();
            currentName = navigator.toString(currentIndex);
            // System.out.println(depth + "->" + currentName);
        }
        // if (stack.size() > 0) {
        stack.pop();
        // }
    }

    /**
     * 
     * @param node
     * @param navigator
     * @throws Exception
     */
    private void addAttributes(VtdNode node, VTDNav navigator) throws Exception {
        if (navigator.getAttrCount() > 0) {
            AutoPilot attrAutoPilot = new AutoPilot(navigator);
            attrAutoPilot.selectAttr(ALL);
            int attributeIndex = -1;
            while ((attributeIndex = attrAutoPilot.iterateAttr()) != -1) {
                String attributeName = navigator.toString(attributeIndex);
                String attributeValue = navigator.toString(attributeIndex + 1);
                node.addAttribute(attributeName, attributeValue);
            }
        }
    }

    /**
     * 
     * @param node
     * @param navigator
     * @throws Exception
     */
    private void addText(VtdNode node, VTDNav navigator) throws Exception {
        int textIndex = navigator.getText();
        if (textIndex != -1) {
            String value = navigator.toString(textIndex).trim();
            if (!value.isEmpty()) {
                VtdNode text = new VtdNode();
                text.type = TEXT_NODE;
                text.value = value;
                text.valid = true;
                node.addChild(text);
            }
        }
    }
}
