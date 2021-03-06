package org.jsystemtest;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import javax.swing.JTable;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import jsystem.framework.scenario.ParameterProvider;
import jsystem.treeui.suteditor.planner.FilterType;
import jsystem.treeui.utilities.CellEditorModel;
import jsystem.utils.beans.CellEditorType;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

public class BeanTreeTableModel extends AbstractTreeTableModel implements CellEditorModel {

	// private Object root;
    private static AbstractBeanTreeNode rootNode;

	private boolean hasChanged;

	static HashSet<String> groups = new HashSet<String>();

    static enum ColNames {
        NAME("Name"), CUR_VAL("Current Value"), DEF_VAL("Default Value"), CLS_NAME("Class Name"), DESC("Description");
        private String columnName;
        private ColNames(String columnName) {
            this.columnName = columnName;
        }

        @Override
        public String toString(){
            return columnName;
        }
    }

	static protected String[] cNames = Arrays.toString(ColNames.values()).replaceAll("\\[|]", "").split(", ");

	public BeanTreeTableModel(DefaultMutableTreeNode root) {
		super(root);
	}

	public int getColumnCount() {
		return cNames.length;
	}

	public String getColumnName(int column) {
		return cNames[column];
	}

	// Moshe's impl.
	public Object getValueAt(Object node, int column) {
		if (!(node instanceof AbstractBeanTreeNode)) {
			return null;
		}
		AbstractBeanTreeNode beanTreeNode = (AbstractBeanTreeNode) node;

		try {
			switch (column) {
			case 0:
				return beanTreeNode.getName();
			case 1: // 'Current value' column
                //return beanTreeNode.getValue();
                Object userObj = beanTreeNode.getUserObject();
                if(userObj != null) {
                    //Class<?> type = userObj.getClass();
                    //return type.isPrimitive() || type == String.class ? beanTreeNode.getValue() : "";
                    return AbstractBeanTreeNode.isTypeNumberOrString(beanTreeNode.objType) ? beanTreeNode.getValue() : "";
                }

			case 2: // 'Default value' column
                Object defaultValue;
                try {
                    defaultValue = beanTreeNode.getDefaultValue().toString();
                } catch (Exception e) {
                    defaultValue = "";
                }
                return defaultValue;
                //return "";
			case 3: // 'Class name' column
                return beanTreeNode.getClassName();
			case 4: // 'Java documentation' column
				// return beanTreeNode.getJavadoc().replaceAll("\n", "\n, ");
				return "";
			}
		} catch (SecurityException se) {

		}

		return null;
	}

    /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
    /*public Class getColumnClass(int c) {
        //return getValueAt(0, c).getClass();
    }*/

    public boolean isCellEditable(Object node, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        if(node instanceof AbstractBeanTreeNode) {
            AbstractBeanTreeNode beanTreeNode = ((AbstractBeanTreeNode) node);
            if (col == ColNames.CUR_VAL.ordinal()) {
                return AbstractBeanTreeNode.isTypeNumberOrString((beanTreeNode).objType) || beanTreeNode.objType.isEnum();
            } /*else if(col == ColNames.NAME.ordinal()) {
                return (beanTreeNode).objType == Boolean.class;
            }*/
        }


        return false;
    }

    public void setValueAt(Object value, Object node, int column) {
        // TODO 4 - check validity
        //System.out.println("Value changed in node " + node.toString() + " : " + value.toString());
        if(node instanceof AbstractBeanTreeNode) {
            AbstractBeanTreeNode beanNode = (AbstractBeanTreeNode)node;

            if(node instanceof BeanRootNode) {
                //beanNode.setUserObject(value);
                beanNode.setValueToRootNode(value);
                modelSupport.firePathChanged(new TreePath(beanNode.getPath()));
            } else {
                try {
                    beanNode.setValueToNonRootNode(value);

                    // Update the row after the change - relevant especially for boolean values
                    AbstractBeanTreeNode parent = ((AbstractBeanTreeNode)(beanNode.getParent()));
                    int nodeIndex = parent.getIndex(beanNode);
                    modelSupport.fireChildChanged(new TreePath(parent.getPath()), nodeIndex, node);
                } catch (IllegalAccessException e) {
                    // TODO 3 - React accordingly
                    System.err.println(e.getMessage());
                } catch (InvocationTargetException e) {
                    // TODO 3 - React accordingly
                    System.err.println(e.getMessage());
                } catch (NoSuchMethodException e) {
                    // TODO 3 - React accordingly
                    System.err.println("Missing Set Method : " + e.getMessage());
                }
            }
        }
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        System.out.println("Value changed to: " + newValue + "\nIn: " + path.toString());
    }

    /*public void setValueAt(Object value, int row, int col) {
        data[row][col] = value;
        fireTableCellUpdated(row, col);
    }*/

	public int getChildCount(Object parent) {
		if (parent instanceof AbstractBeanTreeNode) {
			return ((AbstractBeanTreeNode) parent).getChildCount();
		}
		return 0;
	}

	public Object getChild(Object parent, int index) {
		if (parent instanceof AbstractBeanTreeNode) {
			return ((AbstractBeanTreeNode) parent).getChildAt(index);
		}
		return null;
	}

	/**
	 * Moshe's impl.
	 * 
	 * @param parent
	 * @param child
	 * @return index of child in parent or -1 if no match was found.
	 */
	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof AbstractBeanTreeNode) {
			return ((AbstractBeanTreeNode) parent).getIndex((AbstractBeanTreeNode) child);
		}
		return -1; // Non found
	}

	public void setFilter(String searchText) {
		// TODO Auto-generated method stub

	}

	public void setFilterType(FilterType all) {
		// TODO Auto-generated method stub

	}

	public void setChildToHidden(AbstractBeanTreeNode node) {
        AbstractBeanTreeNode parent = ((AbstractBeanTreeNode)(node.getParent()));
        int nodeIndex = parent.getIndex(node);
        if(parent.hiddenChildren == null) {
            parent.hiddenChildren = new Vector<AbstractBeanTreeNode>();
        }
        parent.remove(node);
        parent.hiddenChildren.add(node);
        node.setParent(null);
        modelSupport.fireChildRemoved(new TreePath(parent.getPath()), nodeIndex, node);
	}



    // Moshe's impl.
    public boolean canMoveUp(AbstractBeanTreeNode selectedNode) {
        AbstractBeanTreeNode parentNode = (AbstractBeanTreeNode) selectedNode.getParent();
        if (null != parentNode) {
            int indexInParent = this.getIndexOfChild(parentNode, selectedNode);
            return (indexInParent > 0);
        }
        return false;
    }

    // Moshe's impl.
    public boolean canMoveDown(AbstractBeanTreeNode selectedNode) {
        AbstractBeanTreeNode parentNode = (AbstractBeanTreeNode) selectedNode.getParent();
        if (null != parentNode) {
            int indexInParent = this.getIndexOfChild(parentNode, selectedNode);
            return (indexInParent < (this.getChildCount(parentNode) - 1));
        }
        return false;
    }

	// Moshe's impl.
	public void moveUp(AbstractBeanTreeNode selectedNode) {
        this.move(selectedNode, true);
	}

	// Moshe's impl.
	public void moveDown(AbstractBeanTreeNode selectedNode) {
        this.move(selectedNode, false);
	}

    private void move(AbstractBeanTreeNode selectedNode, boolean upDirection) {
        AbstractBeanTreeNode parent = (AbstractBeanTreeNode) selectedNode.getParent();
        int selectedNodeIndex = this.getIndexOfChild(parent, selectedNode);
        int switchNodeIndex = selectedNodeIndex + (upDirection ? -1 : 1);
        parent.remove(selectedNodeIndex);
        TreePath parentPath = new TreePath(parent.getPath());

        modelSupport.fireChildRemoved(parentPath, selectedNodeIndex, selectedNode);
        parent.insert(selectedNode, switchNodeIndex);
        modelSupport.fireChildAdded(parentPath, switchNodeIndex, selectedNode);

        if(parent.objType.isArray()) {
            // Also update the original user-object
            selectedNode.substitueArrayElementsOrder(parent, selectedNodeIndex, switchNodeIndex);
        }
    }

    public void addArrayChildNode(AbstractBeanTreeNode parent, int position) {
        //int position = -1; // negative number adds an element at the end of the array - default when user elects "Add" over the array node.
        if(position < 0) {
            position = parent.getChildCount();
        }
        AbstractBeanTreeNode child = parent.generateNewDefaultArrayElementNode();
        if(null != child) {
            try {
                // First update the original user-object
                rootNode.insertNewArrayElementAt(parent, child.getUserObject(), position);

                parent.getVisibleChildren().insertElementAt(child, position);
                child.setParent(parent); // Attach the child to its parent

                TreePath parentPath = new TreePath(parent.getPath());
                modelSupport.fireChildAdded(parentPath, position, child);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InvocationTargetException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public void removeArrayChildNode(AbstractBeanTreeNode node) {
        AbstractBeanTreeNode parent = ((AbstractBeanTreeNode)(node.getParent()));
        int nodeIndex = parent.getIndex(node);

        try {
            // First update the original user-object
            node.removeArrayElement(parent, nodeIndex);

            parent.remove(node);
            node.setParent(null);
            modelSupport.fireChildRemoved(new TreePath(parent.getPath()), nodeIndex, node);
        } catch (IllegalAccessException e) {
            // TODO 3 - React accordingly
            System.err.println(e.getMessage());
        } catch (InvocationTargetException e) {
            // TODO 3 - React accordingly
            System.err.println(e.getMessage());
        } catch (NoSuchMethodException e) {
            // TODO 3 - React accordingly
            System.err.println("Missing Set Method : " + e.getMessage());
        }
    }

    public void removeArrayNodeChildren(AbstractBeanTreeNode parent) {
        int childCount = parent.getChildCount();
        if(childCount > 0) {
            int[] indices = new int[childCount];
            for(int i = 0; i  < childCount; i++) {
                indices[i] = i;
            }

            parent.removeAllChildren();
            modelSupport.fireChildrenRemoved(new TreePath(parent.getPath()), indices, parent.getVisibleChildren().toArray());
        }
    }


	public static BeanTreeTableModel createNewModel(Object root) {
		rootNode = new BeanRootNode(null, AbstractBeanTreeNode.NodeType.ROOT, root.getClass().getSimpleName(),
				root.getClass(), root);
        rootNode.initChildren();
        if(rootNode.objType.isArray()) {
            rootNode.initArrayElementChildren();
        }

		return new BeanTreeTableModel(rootNode);// rootNode);
	}

	public void setHasChanged(boolean hasChanged) {
		this.hasChanged = hasChanged;

	}

	public boolean getHasChanged() {
		return hasChanged;
	}

	public void refresh() {
		//modelSupport.fireNewRoot();
        System.out.println("Refresh: fireNewRoot()");
    }

	public void setChildToVisible(AbstractBeanTreeNode parent, AbstractBeanTreeNode child) {
		int childIndex = -1;
		if ((childIndex = parent.getHiddenChildren().indexOf(child)) < 0) {
			// Child doesn't exist in the hidden children list
			return;
		}
		parent.add(child);
		parent.hiddenChildren.remove(childIndex);
		modelSupport.fireChildAdded(new TreePath(parent.getPath()), parent.getChildCount() - 1, child);
	}

	private static AbstractBeanTreeNode[] getPathToRoot(AbstractBeanTreeNode aNode) {
		List<AbstractBeanTreeNode> path = new ArrayList<AbstractBeanTreeNode>();
		AbstractBeanTreeNode node = aNode;

		while (node != null) {
			path.add(0, node);
			node = (AbstractBeanTreeNode) node.getParent();
			// if (node.getParent() == null){
			// node = null;
			// }else {
			// }

		}
		return path.toArray(new AbstractBeanTreeNode[] {});
	}

	public CellEditorType getEditorType(JTable table, int row, int column) {
        System.out.println("Editor Type: ");
        Object obj = table.getModel().getValueAt(row, column);
        System.out.println(obj.toString());
        //if(objCellEditorType.INT
		return null;
	}

	public String[] getOptions(JTable table, int row, int column) {
		// TODO Auto-generated method stub
		return null;
	}

	// Moshe's impl.
	public Class<?> getCellType(JTable table, int row, int column) {
		/**
		 * TODO 4 - complete getCellType
		 **/
		AbstractBeanTreeNode node = (AbstractBeanTreeNode) ((JXTreeTable) table).getPathForRow(row)
				.getLastPathComponent();

        return node.getObjType();
	}

	public boolean isValidData(JTable table, int row, int column, Object enteredValue) {
		// TODO Auto-generated method stub
		return false;
	}

	public String getLastValidationMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	public ParameterProvider getProvider(JTable table, int row, int column) {
		return null;
	}

}
