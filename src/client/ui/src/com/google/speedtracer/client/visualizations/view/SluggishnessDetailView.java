/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.graphics.client.charts.ColorCodedDataList;
import com.google.gwt.graphics.client.charts.ColorCodedValue;
import com.google.gwt.graphics.client.charts.PieChart;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.MouseOutEvent;
import com.google.gwt.topspin.ui.client.MouseOutListener;
import com.google.gwt.topspin.ui.client.MouseOverEvent;
import com.google.gwt.topspin.ui.client.MouseOverListener;
import com.google.gwt.topspin.ui.client.ResizeEvent;
import com.google.gwt.topspin.ui.client.ResizeListener;
import com.google.gwt.topspin.ui.client.Table;
import com.google.gwt.topspin.ui.client.Widget;
import com.google.speedtracer.client.SourceViewer;
import com.google.speedtracer.client.SymbolServerController;
import com.google.speedtracer.client.SymbolServerService;
import com.google.speedtracer.client.MonitorResources.CommonResources;
import com.google.speedtracer.client.SourceViewer.SourceViewerLoadedCallback;
import com.google.speedtracer.client.SymbolServerController.Callback;
import com.google.speedtracer.client.model.AggregateTimeVisitor;
import com.google.speedtracer.client.model.DomEvent;
import com.google.speedtracer.client.model.EvalScript;
import com.google.speedtracer.client.model.EventRecordType;
import com.google.speedtracer.client.model.EventVisitorTraverser;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.JavaScriptProfile;
import com.google.speedtracer.client.model.LogEvent;
import com.google.speedtracer.client.model.LotsOfLittleEvents;
import com.google.speedtracer.client.model.PaintEvent;
import com.google.speedtracer.client.model.ParseHtmlEvent;
import com.google.speedtracer.client.model.TimerCleared;
import com.google.speedtracer.client.model.TimerFiredEvent;
import com.google.speedtracer.client.model.TimerInstalled;
import com.google.speedtracer.client.model.TypeCountDurationTuple;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.UiEventModel;
import com.google.speedtracer.client.model.XhrLoadEvent;
import com.google.speedtracer.client.model.XhrReadyStateChangeEvent;
import com.google.speedtracer.client.model.EventVisitor.PostOrderVisitor;
import com.google.speedtracer.client.model.EventVisitor.PreOrderVisitor;
import com.google.speedtracer.client.util.Command;
import com.google.speedtracer.client.util.IterableFastStringMap;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.JsIntegerDoubleMap;
import com.google.speedtracer.client.util.JsIntegerMap;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.util.dom.WindowExt;
import com.google.speedtracer.client.view.DetailView;
import com.google.speedtracer.client.view.HoveringPopup;
import com.google.speedtracer.client.view.MainTimeLine;
import com.google.speedtracer.client.visualizations.model.JsStackTrace;
import com.google.speedtracer.client.visualizations.model.JsSymbolMap;
import com.google.speedtracer.client.visualizations.model.LogMessageVisitor;
import com.google.speedtracer.client.visualizations.model.SluggishnessModel;
import com.google.speedtracer.client.visualizations.model.SluggishnessVisualization;
import com.google.speedtracer.client.visualizations.model.JsStackTrace.JsStackFrame;
import com.google.speedtracer.client.visualizations.model.JsSymbolMap.JsSymbol;
import com.google.speedtracer.client.visualizations.view.JavaScriptProfileRenderer.ResizeCallback;
import com.google.speedtracer.client.visualizations.view.JavaScriptProfileRenderer.SourceClickCallback;
import com.google.speedtracer.client.visualizations.view.Tree.ExpansionChangeListener;
import com.google.speedtracer.client.visualizations.view.Tree.Item;
import com.google.speedtracer.client.visualizations.view.Tree.SelectionChangeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DetailsView for sluggishness visualization.
 */
public class SluggishnessDetailView extends DetailView {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String detailsKeyColumn();

    String detailsLayout();

    String detailsTable();

    String detailsTableKey();

    String eventBreakdownHeader();

    String filterPanelIcon();

    String filterPanelMinInput();

    String filterPanelMinLabel();

    String hintletList();

    String logMessageAnnotation();

    String pieChartContainer();

    String sluggishnessPanel();

    int uiPadding();
  }

  /**
   * Filter used on our EventTable.
   */
  public static class EventTableFilter implements EventTable.Filter {
    public EventFilter eventFilter = new EventFilter();

    public EventTableFilter(double durationThreshold) {
      eventFilter.setMinDuration(durationThreshold);
    }

    public boolean shouldFilter(EventTable.TableRow row) {
      EventTable.UiTableRow uiRow = (EventTable.UiTableRow) row;
      return eventFilter.shouldFilter(uiRow.getUiEvent());
    }
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends CurrentSelectionMarker.Resources,
      FilteringScrollTable.Resources, PieChart.Resources, CommonResources,
      HintletRecordsTree.Resources, LazyEventTree.Resources,
      HoveringPopup.Resources, SourceViewer.Resources,
      StackFrameRenderer.Resources, ScopeBar.Resources {
    @Source("resources/magnify-16px.png")
    ImageResource filterPanelIcon();

    @Source("resources/SluggishnessDetailView.css")
    Css sluggishnessDetailViewCss();
  }

  /**
   * The Table of DOM/UI Events.
   */
  class EventTable extends FilteringScrollTable {

    public class UiTableRow extends TableRow {
      private UiEvent uiEvent;

      public UiTableRow(UiEvent uiEvent) {
        super(uiEvent.getDuration());
        this.uiEvent = uiEvent;
      }

      public UiEvent getUiEvent() {
        return this.uiEvent;
      }
    }

    /**
     * Cell that displays an icon if a hintlet is present for this event.
     * 
     * The appearance of the hintlet is changes depending on the most severe
     * hintlet record present.
     * 
     */
    private class AnnotationIconCell extends Cell {
      private JSOArray<HintRecord> hintletRecords;
      private Container iconContainer;

      public AnnotationIconCell(JSOArray<HintRecord> hintletRecords) {
        super("", hintletIconColumnWidth);
        this.hintletRecords = hintletRecords;
      }

      public void refresh(JSOArray<HintRecord> hintletRecords) {
        // The array of hintlet records may have changed.
        // Remove any existing indicators and add a new one.
        getElement().setInnerHTML("");
        this.hintletRecords = hintletRecords;
        if (hintletRecords != null) {
          addIndicator();
        }
      }

      @Override
      protected Element createElement() {
        Element elem = super.createElement();
        iconContainer = new DefaultContainerImpl(elem);
        if (hintletRecords != null) {
          addIndicator();
        }
        return elem;
      }

      private void addIndicator() {
        HintletIndicator indicator = new HintletIndicator(iconContainer,
            hintletRecords, resources);
        setTooltipText(indicator.getTooltipText());
      }
    }

    /**
     * Cell that contains the aggregate stats provided by the UiEventDetails.
     * 
     * Normal Cells have only String contents. This cell needs to be able to
     * lazily grab an Element to display.
     */
    private class BreakDownCell extends Cell {
      private final UiEventDetails statsProvider;

      public BreakDownCell(UiEventDetails statsProvider) {
        super("", -1);
        this.statsProvider = statsProvider;
      }

      @Override
      protected Element createElement() {
        Element elem = super.createElement();
        statsProvider.attachAggregateStatsWidget(elem);
        return elem;
      }
    }

    /**
     * Details component for a UiEvent.
     * 
     * This class contains all the widgetry for the expanded event views. It
     * also exposes a method for getting an element containing aggregate
     * statistics for the UiEvent.
     */
    private class UiEventDetails extends RowDetails {
      private class ProfileClickListener implements ClickListener {
        private int profileType;

        private ProfileClickListener(int profileType) {
          this.profileType = profileType;
        }

        public void onClick(ClickEvent clickEvent) {
          jsProfileRenderer.show(profileType);
          // Defer this so there is no infinite loop
          Command.defer(new Command() {
            @Override
            public void execute() {
              fixHeightOfParentRow();
            }
          });
        }
      }

      private class ResymbolizeClickListener implements ClickListener {
        private JsSymbolMap symbols;
        private JsSymbol sourceSymbol;

        public ResymbolizeClickListener(JsSymbolMap symbols,
            JsSymbol sourceSymbol) {
          this.symbols = symbols;
          this.sourceSymbol = sourceSymbol;
        }

        public void onClick(ClickEvent event) {
          // This is a click on the re-symbolized source
          // symbol. Load the source in the source viewer.
          String sourceUrl = symbols.getSourceServer()
              + sourceSymbol.getResourceBase() + sourceSymbol.getResourceName();

          // TODO(jaimeyap): Put up a spinner or something. It
          // may take a while to load the resource.
          ensureSourceViewer(sourceUrl, new SourceViewerLoadedCallback() {

            public void onSourceFetchFail(int statusCode, SourceViewer viewer) {
              sourceViewer.hide();
            }

            public void onSourceViewerLoaded(SourceViewer viewer) {
              // The viewer should not be loaded at the
              // URL we care about.
              sourceViewer.show();
              sourceViewer.highlightLine(sourceSymbol.getLineNumber());
              sourceViewer.scrollHighlightedLineIntoView();
            }
          });
        }
      }

      private static final int DISPLAYED_EVENTS = 3;
      private ColorCodedDataList aggregateStats;
      private List<ColorCodedValue> data;
      private Table detailsTable;
      private Container detailsTableContainer;
      private final UiEvent event;
      private TableCellElement eventTraceContainerCell;
      private Command heightFixer;
      private Div profileDiv;
      private HintletRecordsTree hintletTree;
      private SourceViewer sourceViewer;
      private JavaScriptProfileRenderer jsProfileRenderer;

      private DivElement treeDiv;

      // Profiles are processed in the background. This variable tells the click
      // handler if the profile needs to be refreshed.
      private boolean javaScriptProfileInProgress = false;

      protected UiEventDetails(UiEvent event, TableRow parent) {
        super(parent);
        this.event = event;
      }

      /**
       * Attaches our aggregate stats widget to a Cell Element. We can only have
       * this attached to a single Cell element.
       * 
       * @param cellElem the {@link Element} for the {@link Cell} we are
       *          attaching to.
       */
      public void attachAggregateStatsWidget(Element cellElem) {
        ensureData();
        assert (aggregateStats == null);
        aggregateStats = new ColorCodedDataList(new DefaultContainerImpl(
            cellElem), data, DISPLAYED_EVENTS, event.getDuration(), true, true,
            resources);
      }

      /**
       * See if any of the data has changed and refresh the view.
       */
      public void refresh() {
        if (!isCreated()) {
          return;
        }
        if (hintletTree == null) {
          hintletTree = createHintletTree(treeDiv);
        } else {
          hintletTree.refresh(event.getHintRecords());
        }
      }

      /**
       * Toggles whether or not the aggregate stats Widget is visible.
       */
      public void toggleAggregateStatsVisibility() {
        if (aggregateStats != null) {
          aggregateStats.setVisible(!aggregateStats.isVisible());
        }
      }

      @Override
      protected Element createElement() {
        Element elem = super.createElement();
        Container myContainer = new DefaultContainerImpl(elem);
        ensureData();

        // Now we need to layout the rest of the row details
        Table detailsLayout = new Table(myContainer);
        detailsLayout.setFixedLayout(true);
        detailsLayout.getElement().setClassName(css.detailsLayout());

        // We have a 1 row, 2 column layout
        TableRowElement row = detailsLayout.insertRow(-1);

        // Create the first column.
        eventTraceContainerCell = row.insertCell(-1);

        // Add the piechart and detailsTable to the second column
        TableCellElement detailsTableCell = row.insertCell(-1);
        detailsTableCell.getStyle().setPropertyPx("paddingRight",
            css.uiPadding());

        // Attach the pie chart.
        detailsTableContainer = new DefaultContainerImpl(detailsTableCell);
        PieChart pieChart = createPieChart(detailsTableContainer);
        int pieChartHeight = pieChart.getElement().getOffsetHeight()
            + css.uiPadding();

        this.detailsTable = createDetailsTable(detailsTableContainer,
            pieChartHeight, event);

        // Now we populate the first column.
        Container eventTraceContainer = new DefaultContainerImpl(
            eventTraceContainerCell);
        treeDiv = eventTraceContainer.getDocument().createDivElement();
        eventTraceContainerCell.appendChild(treeDiv);

        hintletTree = createHintletTree(treeDiv);
        createEventTrace(eventTraceContainer, pieChartHeight);

        profileDiv = new Div(eventTraceContainer);
        updateProfile();

        // Ensure that window resizes don't mess up our row size due to text
        // reflow. Things may need to grow or shrink.
        trackRemover(ResizeEvent.addResizeListener(WindowExt.get(),
            WindowExt.get(), new ResizeListener() {
              public void onResize(ResizeEvent event) {
                if (heightFixer == null && getParentRow().isExpanded()) {
                  heightFixer = new Command() {
                    @Override
                    public void execute() {
                      // We don't want to do this for each resize, but once at
                      // the end.
                      fixHeightOfParentRow();
                      heightFixer = null;
                    }
                  };

                  Command.defer(heightFixer, 200);
                }
              }
            }));
        return elem;
      }

      private void attemptResymbolization(final String resourceUrl,
          final String symbolName, final StackFrameRenderer renderer) {
        // Add resymbolized data to frame/profile if it is available.
        SymbolServerController ssController = getCurrentSymbolServerController();
        if (ssController != null) {
          ssController.requestSymbolsFor(resourceUrl, new Callback() {
            public void onSymbolsFetchFailed(int errorReason) {
              // TODO (jaimeyap): Do something here... or not.
            }

            public void onSymbolsReady(final JsSymbolMap symbols) {
              // Extract the source symbol.
              final JsSymbol sourceSymbol = symbols.lookup(symbolName);

              if (sourceSymbol == null) {
                return;
              }
              // Enhance the rendered frame with the resymbolization.
              renderer.reSymbolize(symbols.getSourceServer(), sourceSymbol,
                  new ResymbolizeClickListener(symbols, sourceSymbol));
            }
          });
        }
      }

      private void buildProfileUi() {
        profileDiv.getElement().setInnerHTML("");
        Container container = new DefaultContainerImpl(profileDiv.getElement());
        HeadingElement profileHeading = container.getDocument().createHElement(
            3);
        profileDiv.getElement().appendChild(profileHeading);
        profileHeading.setInnerText("Profile");
        ScopeBar bar = new ScopeBar(container, resources);
        jsProfileRenderer = new JavaScriptProfileRenderer(container,
            getModel().getJavaScriptProfileForEvent(event),
            new SourceClickCallback() {
              public void onSourceClick(String resourceUrl, final int lineNumber) {
                // TODO(jaimeyap): Put up a spinner or something. It may
                // take a while to load the resource.
                ensureSourceViewer(resourceUrl,
                    new SourceViewerLoadedCallback() {

                      public void onSourceFetchFail(int statusCode,
                          SourceViewer viewer) {
                        sourceViewer.hide();
                      }

                      public void onSourceViewerLoaded(SourceViewer viewer) {
                        // The viewer should not be loaded at the URL we
                        // care about.
                        sourceViewer.show();

                        sourceViewer.highlightLine(lineNumber);
                        sourceViewer.scrollHighlightedLineIntoView();
                      }
                    });
              }
            }, getCurrentSymbolServerController(), new ResizeCallback() {
              public void onResize() {
                fixHeightOfParentRow();
              }
            });
        trackRemover(jsProfileRenderer.getRemover());
        Element flatProfile = bar.add("Flat", new ProfileClickListener(
            JavaScriptProfile.PROFILE_TYPE_FLAT));
        bar.add("Top Down", new ProfileClickListener(
            JavaScriptProfile.PROFILE_TYPE_TOP_DOWN));
        bar.add("Bottom Up", new ProfileClickListener(
            JavaScriptProfile.PROFILE_TYPE_BOTTOM_UP));
        bar.setSelected(flatProfile, true);
      }

      /**
       * Creates the details table information for a single UiEvent selected in
       * the event trace tree.
       * 
       * @param parent The parent {@link Container} that we will be attaching
       *          the table to.
       * @param pieChartHeight The height in pixels of the piechart so that we
       *          can position ourselves accordingly.
       * @param e The {@link UiEvent} that we will be displaying the details of.
       * 
       * @return The {@link Table} that contains the detail information
       */
      private Table createDetailsTable(Container parent, int pieChartHeight,
          final UiEvent e) {
        IterableFastStringMap<String> detailsMap = getDetailsMapForEvent(e);
        final Table table = new Table(parent);

        detailsMap.iterate(new IterableFastStringMap.IterationCallBack<String>() {
          private boolean hasRow = false;

          public void onIteration(String key, String val) {
            // If we have at least one piece of data for this table, we add a
            // header
            if (!hasRow) {
              // Establish column widths.
              Element keyCol = Document.get().createElement("th");
              keyCol.setClassName(css.detailsKeyColumn());
              Element valCol = Document.get().createElement("th");
              table.getTableHead().appendChild(keyCol);
              table.getTableHead().appendChild(valCol);

              // Now add the title
              Element titleRow = Document.get().createElement("tr");
              Element title = Document.get().createElement("th");
              title.setAttribute("colspan", "2");
              title.setAttribute("align", "left");
              title.setInnerText("Details for "
                  + EventRecordType.typeToDetailedTypeString(e));
              title.getStyle().setWidth(100, Unit.PX);
              titleRow.appendChild(title);
              table.getTableHead().appendChild(titleRow);
              hasRow = true;
            }

            TableRowElement row = table.appendRow();
            TableCellElement cell = row.insertCell(-1);
            cell.setClassName(css.detailsTableKey());
            String rowKey = key.substring(1);
            cell.setInnerText(rowKey);
            cell = row.insertCell(-1);
            fillDetailRowValue(cell, rowKey, val);
          }

          /**
           * Populates the value cell for a Row in the Details Table for a
           * single node.
           */
          private void fillDetailRowValue(TableCellElement cell, String key,
              String val) {
            if (key.equals(STACK_TRACE_KEY)) {
              formatStackTrace(cell, val);
            } else {
              cell.setInnerText(val);
            }
          }
        });

        table.addStyleName(css.detailsTable());
        // ensure that the table is positioned below the pieChart
        table.getElement().getStyle().setPropertyPx("marginTop", pieChartHeight);
        return table;
      }

      private Tree createEventTrace(Container parent, final int pieChartHeight) {
        Widget header = new Widget(parent.getDocument().createHElement(2),
            parent) {
        };
        header.setStyleName(css.eventBreakdownHeader());
        header.getElement().setInnerText("Event Trace");

        final LazyEventTree tree = new LazyEventTree(parent, event, resources);

        // Hook listeners to tree list to monitor selection changes and
        // expansion changes.
        tree.addSelectionChangeListener(new SelectionChangeListener() {
          Element offsetParent = getParentRow().getElement();

          public void onSelectionChange(ArrayList<Item> selected) {
            // Wipe the old table
            detailsTable.destroy();

            // Sort the nodes by start time.
            Collections.sort(selected, new Comparator<Item>() {

              public int compare(Item node1, Item node2) {
                UiEvent e1 = (UiEvent) node1.getItemTarget();
                UiEvent e2 = (UiEvent) node2.getItemTarget();

                return Double.compare(e1.getTime(), e2.getTime());
              }
            });

            Item newSelection = selected.get(selected.size() - 1);
            // Find how far to move table down to current selection.
            // We have to recursively walk up to compute the correct offset top.
            // We will encounter the UI padding two extra times along the way
            // crossing the tree boundary and crossing the details div boundary,
            // totally 3 encounters with padding we have to account for.
            int minTableOffset = Math.max(pieChartHeight,
                recursiveGetOffsetTop(newSelection.getElement())
                    - (3 * css.uiPadding()));

            if (selected.size() == 1) {
              // We have a single selection. Simply display the details for the
              // single node.
              detailsTable = createDetailsTable(detailsTableContainer,
                  minTableOffset, (UiEvent) newSelection.getItemTarget());

            } else {
              // Display aggregate information over the range of nodes.
              detailsTable = createMultiNodeDetailsTable(detailsTableContainer,
                  minTableOffset, selected);
            }

            fixHeightOfParentRow();
          }

          private int recursiveGetOffsetTop(Element node) {
            if (node == null || node.getOffsetParent() == null
                || node.equals(offsetParent)) {
              return 0;
            } else {
              return node.getOffsetTop()
                  + recursiveGetOffsetTop(node.getOffsetParent());
            }
          }

        });

        tree.addExpansionChangeListener(new ExpansionChangeListener() {

          public void onExpansionChange(Item changedItem) {
            fixHeightOfParentRow();
          }

        });

        // We make sure to have the tree cleaned up when we clean up ourselves.
        trackRemover(tree.getRemover());

        return tree;
      }

      private HintletRecordsTree createHintletTree(DivElement parent) {
        if (!event.hasHintRecords()) {
          return null;
        }

        JSOArray<HintRecord> hintlets = event.getHintRecords();

        // Create a wrapper around the tree so it doesn't spill over the
        // piechart to the right.
        parent.setClassName(css.hintletList());
        parent.getStyle().setPropertyPx("width",
            computedPieChartLeftOffset - (2 * css.uiPadding()));

        HintletRecordsTree tree = new HintletRecordsTree(
            new DefaultContainerImpl(parent), hintlets, resources);

        // Hook listener to tree list to monitor expansion changes.
        tree.addExpansionChangeListener(new ExpansionChangeListener() {

          public void onExpansionChange(Item changedItem) {
            fixHeightOfParentRow();
          }

        });

        // We make sure to have the tree cleaned up when we clean up ourselves.
        trackRemover(tree.getRemover());

        return tree;
      }

      private Table createMultiNodeDetailsTable(Container parent,
          int pieChartHeight, ArrayList<Item> selectedNodes) {
        Table table = new Table(parent);
        table.setFixedLayout(true);
        table.addStyleName(css.detailsTable());

        // Assume that List is sorted.
        UiEvent earliest = (UiEvent) selectedNodes.get(0).getItemTarget();
        UiEvent latest = (UiEvent) selectedNodes.get(selectedNodes.size() - 1).getItemTarget();
        double delta = latest.getTime() - earliest.getTime();

        TableRowElement row = table.appendRow();
        TableCellElement cell = row.insertCell(-1);
        cell.setClassName(css.detailsTableKey());
        cell.setInnerText("Time Delta");
        cell = row.insertCell(-1);
        cell.setInnerText(TimeStampFormatter.formatMilliseconds(delta));

        // ensure that the table is positioned below the pieChart
        table.getElement().getStyle().setPropertyPx("marginTop", pieChartHeight);
        return table;
      }

      private PieChart createPieChart(Container parent) {
        // We put an extra div in there to center our piechart and to apply
        // the rounded corners and backing layer styles underneath the piechart.
        Div centeringDiv = new Div(parent);
        centeringDiv.addStyleName(css.pieChartContainer());
        PieChart chart = new PieChart(centeringDiv, data, resources);
        chart.showLegend();

        return chart;
      }

      private void ensureData() {
        if (data == null) {
          data = new ArrayList<ColorCodedValue>();
          // visitor that aggregates time
          AggregateTimeVisitor aggregateTimeVisitor = new AggregateTimeVisitor(
              event);
          // visitor that white lists parents of log messages
          LogMessageVisitor whiteListVisitor = new LogMessageVisitor();

          // We know that if this already exists, visitors have already run
          if (!aggregateTimeVisitor.alreadyApplied()) {
            PreOrderVisitor[] preOrderVisitors = {whiteListVisitor};
            PostOrderVisitor[] postOrderVisitors = {aggregateTimeVisitor};
            EventVisitorTraverser.traverse(preOrderVisitors, event,
                postOrderVisitors);
          }

          if (event.hasUserLogs()) {
            // Annotate the parent row
            TableRow parentRow = getParentRow();
            List<Cell> parentRowCells = parentRow.getCells();
            Cell annotationIconCell = parentRowCells.get(0);
            annotateCellWithLogIcon(annotationIconCell);
          }

          JsIntegerDoubleMap durations = aggregateTimeVisitor.getTypeDurations();
          assert (durations != null);

          durations.iterate(new JsIntegerDoubleMap.IterationCallBack() {

            public void onIteration(int key, double val) {
              if (val > 0) {
                data.add(new ColorCodedValue(EventRecordType.typeToString(key),
                    val, EventRecordColors.getColorForType(key)));
              }
            }

          });

          Collections.sort(data);
        }
      }

      /**
       * Make sure the source viewer exists and is loaded at the specified
       * resource URL.
       */
      private void ensureSourceViewer(String resourceUrl,
          final SourceViewerLoadedCallback callback) {
        if (sourceViewer == null) {
          // Attach the container above the table so that the source viewer is
          // positioned independent of the table scroll and of the currently
          // viewed row.
          SourceViewer.create(getTableContents().getParentElement(),
              resourceUrl, resources, new SourceViewerLoadedCallback() {

                public void onSourceFetchFail(int statusCode,
                    SourceViewer viewer) {
                  // TODO(jaimeyap): Indicate that the source was
                  // unable to be fetched. For now, simply do not
                  // attempt to show the sourceViewer.
                }

                public void onSourceViewerLoaded(SourceViewer viewer) {
                  UiEventDetails.this.sourceViewer = viewer;
                  // Position the source viewer so that it fills half the
                  // details view. Below the table header, and flush with the
                  // bottom of the window.
                  viewer.getElement().getStyle().setTop(
                      resources.filteringScrollTableCss().headerHeight() + 1,
                      Unit.PX);
                  // Half the width with a little space for border of the table.
                  viewer.getElement().getStyle().setRight(51, Unit.PCT);

                  // Now forward to the callback.
                  callback.onSourceViewerLoaded(viewer);
                }
              });
        } else {
          sourceViewer.loadResource(resourceUrl, callback);
        }
      }

      private void fixHeightOfParentRow() {
        // Our height should be the size of the details panel + the row height
        int height = getElement().getOffsetHeight()
            + resources.filteringScrollTableCss().rowHeight();
        getParentRow().getElement().getStyle().setPropertyPx("height", height);
      }

      private void formatStackTrace(TableCellElement cell, String val) {
        JsStackTrace stackTrace = JsStackTrace.create(val);
        List<JsStackFrame> frames = stackTrace.getFrames();
        for (int i = 0, n = frames.size(); i < n; i++) {
          final JsStackFrame frame = frames.get(i);
          final StackFrameRenderer frameRenderer = new StackFrameRenderer(cell,
              frame, resources);
          trackRemover(frameRenderer.getRemover());
          renderStackFrame(frame, frameRenderer);
        }
      }

      private void renderStackFrame(final JsStackFrame frame,
          final StackFrameRenderer frameRenderer) {
        final String resourceUrl = frame.getResourceUrl();

        // TODO(zundel) this click listener could be consolidated with the
        // JavaScriptProfileRenderer's click listener.
        frameRenderer.renderFrame(new ClickListener() {
          public void onClick(ClickEvent event) {
            // TODO(jaimeyap): Put up a spinner or something. It may
            // take a while to load the resource.
            ensureSourceViewer(resourceUrl, new SourceViewerLoadedCallback() {

              public void onSourceFetchFail(int statusCode, SourceViewer viewer) {
                sourceViewer.hide();
              }

              public void onSourceViewerLoaded(SourceViewer viewer) {
                // The viewer should not be loaded at the URL we
                // care about.
                sourceViewer.show();
                sourceViewer.highlightLine(frame.getLineNumber());
                sourceViewer.markColumn(frame.getLineNumber(),
                    frame.getColNumber());
                sourceViewer.scrollColumnMarkerIntoView();
              }
            });
          }
        });

        attemptResymbolization(frame.getResourceUrl(), frame.getSymbolName(),
            frameRenderer);
      }

      /**
       * Conditionally puts the profile UI below the trace tree.
       */
      private void updateProfile() {
        if (event.hasJavaScriptProfile()) {
          javaScriptProfileInProgress = false;
          buildProfileUi();
        } else if (event.processingJavaScriptProfile()) {
          this.javaScriptProfileInProgress = true;
          profileDiv.setHtml("<h3>Profile</h3><div><i>Processing...</i></div>");
        } else {
          profileDiv.setHtml("");
        }
      }
    }

    /**
     * A data bag to store in the record map that tracks a row in the
     * EventTable.
     */
    private class UiEventRow {
      public AnnotationIconCell annotationIconCell;
      public UiEvent event;
      public UiEventDetails uiEventDetails;

      public UiEventRow(UiEvent event, AnnotationIconCell annotationIconCell,
          UiEventDetails uiEventDetails) {
        this.event = event;
        this.annotationIconCell = annotationIconCell;
        this.uiEventDetails = uiEventDetails;
      }
    }

    private static final String STACK_TRACE_KEY = "Stack Trace";

    public SluggishnessEventFilterPanel eventFilterPanel;

    // The index of the first event in the table model.
    private int beginIndex = 0;

    // The index of the last event in the table model.
    private int endIndex = 0;

    // A map of all rows in the table keyed by the record sequence number
    private JsIntegerMap<UiEventRow> recordMap = JsIntegerMap.createObject().cast();

    private RowListener rowListener;

    public EventTable(Container container, EventTableFilter filter,
        Resources resources) {
      super(container, filter, resources);

      rowListener = new RowListener() {

        public void onClick(ClickEvent event) {
          TableRow row = (TableRow) event.getSource();
          row.toggleDetails();
          UiEventDetails details = (UiEventDetails) row.getDetails();
          details.toggleAggregateStatsVisibility();
          if (details.javaScriptProfileInProgress) {
            details.updateProfile();
          }
        }

        public void onMouseOver(MouseOverEvent event) {
          UiEvent e = (UiEvent) event.getSource();
          assert e != null;
          String description = EventRecordType.typeToDetailedTypeString(e)
              + " @" + TimeStampFormatter.format(e.getTime());
          getVisualization().getCurrentEventMarkerModel().update(e.getTime(),
              e.getDuration(), description, 0);
        }
      };

      eventFilterPanel = new SluggishnessEventFilterPanel(
          getFilterPanelContainer(), this, filter.eventFilter, css, getModel());
    }

    /**
     * This method handles type conversion and calls corresponding
     * {@link #fillRow} method for the specific event type.
     * 
     * @param e the event
     * @param append to append or not to append
     */
    public TableRow addRowForUiEvent(UiEvent e, boolean append) {
      TableRow row;
      if (append) {
        row = contentTable.appendRow(e);
      } else {
        // stick on the front
        row = contentTable.prependRow(e);
      }

      UiEventDetails details = new UiEventDetails(e, row);
      // Fill in the row
      fillRowForUiEvent(e, EventRecordType.typeToDetailedTypeString(e),
          details, row);

      // Add mouse over listener
      row.addMouseOverListener(e, rowListener);
      row.addClickListener(row, rowListener);

      return row;
    }

    public UiTableRow appendRow(UiEvent uiEvent) {
      UiTableRow row = new UiTableRow(uiEvent);
      insertRow(row, true);
      return row;
    }

    public UiTableRow prependRow(UiEvent uiEvent) {
      UiTableRow row = new UiTableRow(uiEvent);
      insertRow(row, false);
      return row;
    }

    public void refreshRecord(UiEvent uiEvent) {
      UiEventRow row = this.recordMap.get(uiEvent.getSequence());
      if (row == null) {
        return;
      }
      row.annotationIconCell.refresh(row.event.getHintRecords());
      row.uiEventDetails.refresh();

      // If this row has Log messages, it would already by whitelisted. We need
      // to re-add the annotation icon on refresh.
      if (uiEvent.hasUserLogs()) {
        annotateCellWithLogIcon(row.annotationIconCell);
      }
    }

    /**
     * Wipes the table and re-adds events in the current page.
     */
    @Override
    public void renderTable() {
      // Cancel any pending resymbolization requests.
      // Add resymbolized frame if it is available.
      SymbolServerController ssController = getCurrentSymbolServerController();
      if (ssController != null) {
        ssController.cancelPendingRequests();
      }

      // Clear out the attached tr elements from the tbody.
      clearTable();

      // Clear the map of record number to table rows
      recordMap = JsIntegerMap.createObject().cast();

      for (int i = beginIndex; i < endIndex; i++) {
        addRowForUiEvent(getModel().getEventList().get(i), true);
      }

      // Actually add the rows to the dom.
      super.renderTable();
    }

    /**
     * Sets the total range of events viewable by the table. Resets the view to
     * the first page.
     * 
     * @param beginIndex
     * @param endIndex
     */
    public void updateTotalTableRange(int beginIndex, int endIndex) {
      this.beginIndex = beginIndex;
      this.endIndex = endIndex;
      renderTable();
    }

    private void annotateCellWithLogIcon(Cell toAnnotate) {
      DivElement infoBubble = DocumentExt.get().createDivWithClassName(
          css.logMessageAnnotation());
      toAnnotate.getElement().appendChild(infoBubble);
    }

    private AnnotationIconCell createAnnotationIconCell(TableRow row,
        JSOArray<HintRecord> hintlets) {
      AnnotationIconCell iconCell = new AnnotationIconCell(hintlets);
      row.addCell(iconCell);
      return iconCell;
    }

    /**
     * Creates and attaches a {@link BreakDownCell} to a {@link TableRow}.
     * 
     * @param row the {@link TableRow} we are attaching to
     * @param details the {@link UiEventDetails} that has the data and widgetry
     *          for populating the BreakDownCell.
     * @return
     */
    private Cell createBreakDownCell(TableRow row, UiEventDetails details) {
      BreakDownCell cell = new BreakDownCell(details);
      row.addCell(cell);
      return cell;
    }

    /**
     * Creates and adds a Cell in the Table.
     * 
     * @param row the row we are inserting a cell into
     * @param contents the String contents we are sticking into the cell
     * @param width the explicit width in pixels we want to cell to be.
     *          Specifying -1 here will cause the cell to take up the entire
     *          width of its parent.
     * @return
     */
    private Cell createTableCell(TableRow row, String contents, int width) {
      Cell cell = new Cell(contents, width);
      row.addCell(cell);
      return cell;
    }

    private void fillRowForUiEvent(UiEvent uiEvent, String type,
        UiEventDetails details, TableRow row) {

      // Hintlet/info bubble icon
      AnnotationIconCell annotationCell = createAnnotationIconCell(row,
          uiEvent.getHintRecords());

      // Start Time
      createTableCell(row, "@" + TimeStampFormatter.format(uiEvent.getTime()),
          startTimeColumnWidth);

      // Duration
      String duration = TimeStampFormatter.formatMilliseconds(uiEvent.getDuration());

      createTableCell(row, duration, durationColumnWidth);

      // Type
      createTableCell(row, type, typeColumnWidth);

      // Details
      createBreakDownCell(row, details);

      recordMap.put(uiEvent.getSequence(), new UiEventRow(uiEvent,
          annotationCell, details));
    }

    private SymbolServerController getCurrentSymbolServerController() {
      SluggishnessModel sModel = (SluggishnessModel) getVisualization().getModel();
      String resourceUrl = sModel.getCurrentUrl();
      return SymbolServerService.getSymbolServerController(new Url(resourceUrl));
    }

    /**
     * Goes to concrete implementation to construct details map for an event.
     * 
     * @param e the {@link UiEvent}
     * @return the details Map for the UiEvent
     */
    private IterableFastStringMap<String> getDetailsMapForEvent(UiEvent e) {
      IterableFastStringMap<String> details = new IterableFastStringMap<String>();

      details.put("Description", e.getHelpString());
      details.put("@", TimeStampFormatter.formatMilliseconds(e.getTime()));
      if (e.getDuration() > 0) {
        details.put("Duration", TimeStampFormatter.formatMilliseconds(
            e.getDuration(), 3));
      }
      String backTrace = e.getBackTrace();
      if (backTrace != null) {
        details.put(STACK_TRACE_KEY, backTrace);
      }

      switch (e.getType()) {
        case DomEvent.TYPE:
          // TODO(jaimeyap): Re-instrument the following.
          /*
           * DomEvent domEvent = e.cast(); details.put( "Capture Duration",
           * TimeStampFormatter
           * .formatMilliseconds(domEvent.getCaptureDuration())); details.put(
           * "Bubble Duration",
           * TimeStampFormatter.formatMilliseconds(domEvent.getBubbleDuration
           * ()));
           */
          break;
        case LotsOfLittleEvents.TYPE:
          JSOArray<TypeCountDurationTuple> tuples = e.<LotsOfLittleEvents> cast().getTypeCountDurationTuples();
          for (int i = 0, n = tuples.size(); i < n; i++) {
            TypeCountDurationTuple tuple = tuples.get(i);
            details.put(EventRecordType.typeToString(tuple.getType()),
                "count: "
                    + tuple.getCount()
                    + " duration: "
                    + TimeStampFormatter.formatMilliseconds(
                        tuple.getDuration(), 3));
          }
          break;
        case TimerFiredEvent.TYPE:
          TimerInstalled timerData = sourceModel.getTimerMetaData(e.<TimerInstalled> cast().getTimerId());
          populateDetailsForTimerInstall(timerData, details);
          break;
        case TimerInstalled.TYPE:
          populateDetailsForTimerInstall(e.<TimerInstalled> cast(), details);
          break;
        case TimerCleared.TYPE:
          details.put("Cleared Timer Id", e.<TimerCleared> cast().getTimerId()
              + "");
          break;
        case PaintEvent.TYPE:
          PaintEvent paintEvent = e.cast();
          details.put("Origin", paintEvent.getX() + ", " + paintEvent.getY());
          details.put("Size", paintEvent.getWidth() + " x "
              + paintEvent.getHeight());
          break;
        case ParseHtmlEvent.TYPE:
          // TODO(jaimeyap): Re-instrument the following.
          /*
           * ParseHtmlEvent parseHtmlEvent = e.cast(); details.put("Url",
           * parseHtmlEvent.getURL()); details.put("Line Number",
           * parseHtmlEvent.getLineNumber() + "");
           */
          break;
        case EvalScript.TYPE:
          EvalScript scriptTagEvent = e.cast();
          details.put("Url", scriptTagEvent.getURL());
          details.put("Line Number", scriptTagEvent.getLineNumber() + "");
          break;
        case XhrReadyStateChangeEvent.TYPE:
          XhrReadyStateChangeEvent xhrEvent = e.cast();
          details.put("Ready State", xhrEvent.getReadyState() + "");
          details.put("Url", xhrEvent.getUrl());
          break;
        case XhrLoadEvent.TYPE:
          details.put("Url", e.<XhrLoadEvent> cast().getUrl());
          break;
        case LogEvent.TYPE:
          LogEvent logEvent = e.cast();
          details.put("Message", logEvent.getMessage());
          break;
        default:
          break;
      }

      if (e.getOverhead() > 0) {
        details.put("Overhead", TimeStampFormatter.formatMilliseconds(
            e.getOverhead(), 2));
      }

      return details;
    }

    private void populateDetailsForTimerInstall(TimerInstalled timerData,
        IterableFastStringMap<String> details) {
      if (timerData != null) {
        details.put("Timer Id", timerData.getTimerId() + "");
        details.put("Timer Type", timerData.isSingleShot() ? "setTimeout"
            : "setInterval");
        details.put("Interval", timerData.getInterval() + "ms");
      }
    }
  }

  /**
   * Handles events for a row.
   */
  private interface RowListener extends MouseOverListener, ClickListener {
  }

  // These are widths before padding
  private static final int durationColumnWidth = 100;

  private static final int hintletIconColumnWidth = 24;

  private static final int startTimeColumnWidth = 90;

  private static final double TOP_LEVEL_FILTER_THRESHOLD = 3;

  // The width of the type column in the table
  private static final int typeColumnWidth = 125;

  private final int computedPieChartLeftOffset;

  private final EventTable contentTable;

  private final SluggishnessDetailView.Css css;

  private final SluggishnessDetailView.Resources resources;

  private UiEventModel sourceModel;

  public SluggishnessDetailView(Container parent,
      SluggishnessVisualization viz, MainTimeLine timeLine,
      UiEventModel sourceModel, final SluggishnessDetailView.Resources resources) {
    super(parent, viz);
    this.resources = resources;
    css = resources.sluggishnessDetailViewCss();
    Element elem = getElement();
    elem.setClassName(css.sluggishnessPanel());
    this.sourceModel = sourceModel;

    EventTableFilter filter = new EventTableFilter(TOP_LEVEL_FILTER_THRESHOLD);
    contentTable = new EventTable(new DefaultContainerImpl(elem), filter,
        resources);

    // Define our header Cells
    contentTable.addColumn("", hintletIconColumnWidth);
    contentTable.addColumn("Started", startTimeColumnWidth);
    contentTable.addColumn("Duration", durationColumnWidth);
    contentTable.addColumn("Type", typeColumnWidth);
    contentTable.addColumn("Breakdown by Time", -1);

    // Magnifying glass shaped icon that is used to expose the filter
    // settings.
    Div filterPanelIcon = new Div(contentTable.getContainer());
    filterPanelIcon.setStyleName(css.filterPanelIcon());
    filterPanelIcon.getElement().setAttribute("title", "Hide/Show Filter Panel");
    filterPanelIcon.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        contentTable.eventFilterPanel.toggleFilterPanelVisible(getModel());
      }
    });

    // Add a mouse out listener to turn off the current event marker when
    // the cursor leaves the detail view.
    MouseOutEvent.addMouseOutListener(this, this.getElement(),
        new MouseOutListener() {
          public void onMouseOut(MouseOutEvent event) {
            getVisualization().getCurrentEventMarkerModel().setNoSelection();
          }
        });

    // Start with the selection hidden.
    getVisualization().getCurrentEventMarkerModel().setNoSelection();

    // The piechart is offset by the fixed sizes of the columns and their
    // padding
    this.computedPieChartLeftOffset = hintletIconColumnWidth
        + startTimeColumnWidth + durationColumnWidth + typeColumnWidth
        + (4 * resources.filteringScrollTableCss().headerCellPadding());
  }

  public void refreshRecord(UiEvent uiEvent) {
    contentTable.refreshRecord(uiEvent);
  }

  /**
   * Immediately adds and renders an event to the table. This is important for
   * the initial window where we may have events streaming in at a high rate.
   * Doing a full binary search to rebuild the table on each event would be bad.
   * We can get away with an append to the table.
   * 
   * @param searchableEvent
   */
  public void shortCircuitAddEvent(UiEvent event) {
    contentTable.addRowForUiEvent(event, true);
    // The row will not be rendered if it is already attached.
    contentTable.renderRow(contentTable.getLastRow());
  }

  /**
   * Takes in the left and right boundaries of the window we want to display.
   * Figures out the DOM/UI Events that fall within the window.
   * 
   * The algorithm for performing this search is as follows: Do a binary search
   * to find the last element that falls within the window. Linear walk
   * backwards to the start of the window.
   */
  public void updateView(double left, double right, boolean noForceReDisplay) {
    int[] indices = getModel().getIndexesOfEventsInRange(left, right, false);

    if (indices != null) {
      if (indices.length == 0) {
        // blank the table
        contentTable.updateTotalTableRange(0, 0);
      } else {
        // Now we know that endIndex is the last event in the window,
        // and eventIndex has been walked back past the start of the window.
        // If we get a valid endIndex, then eventIndex is guaranteed to be 1 too
        // far past the window.
        contentTable.updateTotalTableRange(indices[0], indices[1]);
      }
    }
  }

  private SluggishnessModel getModel() {
    return ((SluggishnessVisualization) getVisualization()).getModel();
  }
}
