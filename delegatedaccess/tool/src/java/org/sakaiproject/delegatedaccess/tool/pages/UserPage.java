package org.sakaiproject.delegatedaccess.tool.pages;



import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.tree.AbstractTree;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.delegatedaccess.model.NodeModel;
import org.sakaiproject.delegatedaccess.model.ToolSerialized;
import org.sakaiproject.site.api.Site;

/**
 * Creates the landing page for a user to show them all their access and links to go to the sites
 * 
 * @author Bryan Holladay (holladay@longsight.com)
 *
 */
public class UserPage  extends BaseTreePage{

	private BaseTree tree;
	boolean expand = true;
	private String search = "";

	protected AbstractTree getTree()
	{
		return tree;
	}

	public UserPage(){
		disableLink(firstLink);

		//this is the home page so set user as current user
		String userId = sakaiProxy.getCurrentUserId();

		//tree:

		//Expand/Collapse Link
		add(getExpandCollapseLink());

		final TreeModel treeModel = projectLogic.createTreeModelForUser(userId, false, true);

		//a null model means the user doesn't have any associations
		tree = new LinkTree("tree", treeModel){
			@Override
			public boolean isVisible() {
				return treeModel != null;
			}
			protected void onNodeLinkClicked(Object node, BaseTree tree, AjaxRequestTarget target) {
				if(!tree.getTreeState().isNodeExpanded(node)){
					tree.getTreeState().expandNode(node);
				}else{
					tree.getTreeState().collapseNode(node);
				}

				if(tree.isLeaf(node)){
					//The user has clicked a leaf and chances are its a site.
					//all sites are leafs, but there may be non sites as leafs
					NodeModel nodeModel = (NodeModel) ((DefaultMutableTreeNode) node).getUserObject();
					if(nodeModel.getNode().description != null && nodeModel.getNode().description.startsWith("/site/")){
						Site site = sakaiProxy.getSiteByRef(nodeModel.getNode().description);
						if(site != null){
							//ensure the access for this user has been granted
							projectLogic.grantAccessToSite(nodeModel);
							//redirect the user to the site
							target.appendJavascript("top.location='" + site.getUrl() + "'");
						}
					}
				}
			};
		};
		tree.setRootLess(true);
		add(tree);
		tree.getTreeState().collapseAll();

		//Access Warning:
		Label noAccessLabel = new Label("noAccess"){
			@Override
			public boolean isVisible() {
				return treeModel == null;
			}
		};

		noAccessLabel.setDefaultModel(new StringResourceModel("noDelegatedAccess", null));        
		add(noAccessLabel);


		//Create Search Form:
		final PropertyModel<String> messageModel = new PropertyModel<String>(this, "search");
		Form<?> form = new Form("form"){
			@Override
			protected void onSubmit() {	
				setResponsePage(new UserPageSiteSearch(search, treeModel));
			}
		};
		form.add(new TextField<String>("search", messageModel){
			@Override
			public boolean isVisible() {
				return treeModel != null;
			}
		});
		form.add(new WebMarkupContainer("searchHeader"){
			@Override
			public boolean isVisible() {
				return treeModel != null;
			};
		});
		form.add(new Button("submitButton"){
			@Override
			public boolean isVisible() {
				return treeModel != null;
			}
		});
		
		add(form);
		

	}

}
