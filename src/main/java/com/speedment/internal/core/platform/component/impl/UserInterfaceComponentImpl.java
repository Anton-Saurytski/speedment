/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.internal.core.platform.component.impl;

import com.speedment.Speedment;
import com.speedment.component.UserInterfaceComponent;
import com.speedment.config.db.trait.HasMainInterface;
import com.speedment.internal.license.OpenSourceLicense;
import com.speedment.internal.license.AbstractSoftware;
import com.speedment.internal.ui.brand.DefaultBrand;
import com.speedment.internal.ui.config.DocumentProperty;
import com.speedment.license.Software;
import com.speedment.stream.MapStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.controlsfx.control.PropertySheet;
import static com.speedment.util.NullUtil.requireNonNulls;
import static java.util.Objects.requireNonNull;
import static javafx.collections.FXCollections.observableArrayList;

/**
 *
 * @author Emil Forslund
 */
public final class UserInterfaceComponentImpl extends InternalOpenSourceComponent implements UserInterfaceComponent {
    
    private final static String[] DEFAULT_STYLESHEETS = {"/css/speedment.css"};
    
    private final ObservableList<PropertySheet.Item> properties;
    private final ObservableList<Node> outputMessages;
    private final ObservableList<TreeItem<DocumentProperty>> selectedTreeItems;
    private final Map<Class<?>, List<UserInterfaceComponent.ContextMenuBuilder<?>>> contextMenuBuilders;
    private final List<String> stylesheets;
    private Brand brand;
    
    public UserInterfaceComponentImpl(Speedment speedment) {
        super(speedment);
        properties          = observableArrayList();
        outputMessages      = observableArrayList();
        selectedTreeItems   = observableArrayList();
        contextMenuBuilders = new ConcurrentHashMap<>();
        stylesheets         = new CopyOnWriteArrayList<>(DEFAULT_STYLESHEETS);
        
        brand = new DefaultBrand(
            "Speedment", 
            "Open Source", 
            "/images/logo.png",
            "/images/speedment_open_source_small.png"
        );
    }

    @Override
    public ObservableList<PropertySheet.Item> getProperties() {
        return properties;
    }

    @Override
    public ObservableList<TreeItem<DocumentProperty>> getSelectedTreeItems() {
        return selectedTreeItems;
    }

    @Override
    public ObservableList<Node> getOutputMessages() {
        return outputMessages;
    }
    
    @Override
    public void setBrand(Brand brand) {
        this.brand = requireNonNull(brand);
    }

    @Override
    public Brand getBrand() {
        return brand;
    }
    
    @Override
    public <DOC extends DocumentProperty & HasMainInterface> void installContextMenu(Class<? extends DOC> nodeType, ContextMenuBuilder<DOC> menuBuilder) {
        contextMenuBuilders.computeIfAbsent(nodeType, k -> new CopyOnWriteArrayList<>()).add(menuBuilder);
    }

    @Override
    public <DOC extends DocumentProperty & HasMainInterface> Optional<ContextMenu> createContextMenu(TreeCell<DocumentProperty> treeCell, DOC doc) {
        requireNonNulls(treeCell, doc);
        
        @SuppressWarnings("unchecked")
        final List<UserInterfaceComponent.ContextMenuBuilder<DOC>> builders = 
            (List<UserInterfaceComponent.ContextMenuBuilder<DOC>>) 
            MapStream.of(contextMenuBuilders)
                .filterKey(clazz -> clazz.isAssignableFrom(doc.getClass()))
                .values()
                .flatMap(List::stream)
                .map(builder -> (UserInterfaceComponent.ContextMenuBuilder<DOC>) builder)
                .collect(toList());
        
        final ContextMenu menu = new ContextMenu();
        for (int i = 0; i < builders.size(); i++) {
            final List<MenuItem> items = builders.get(i)
                .build(treeCell, doc)
                .collect(toList());
            
            if (i > 0 && !items.isEmpty()) {
                menu.getItems().add(new SeparatorMenuItem());
            }
            
            items.forEach(menu.getItems()::add);
        }
        
        if (menu.getItems().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(menu);
        }
    }

    @Override
    public Stream<String> stylesheetFiles() {
        return stylesheets.stream();
    }

    @Override
    public void addStylesheetFile(String filename) {
        this.stylesheets.add(filename);
    }

    @Override
    public Stream<Software> getDependencies() {
        return Stream.of(DEPENDENCIES);
    }
    
    private final static Software[] DEPENDENCIES = {
        AbstractSoftware.with("Silk",          "1.3",     OpenSourceLicense.CC_BY_2_5),
        AbstractSoftware.with("ControlsFX",    "8.40.10", OpenSourceLicense.BSD_3_CLAUSE),
        AbstractSoftware.with("FontawesomeFX", "8.8",     OpenSourceLicense.APACHE_2),
    };
}