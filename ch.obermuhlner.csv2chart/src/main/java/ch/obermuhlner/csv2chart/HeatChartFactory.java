package ch.obermuhlner.csv2chart;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import ch.obermuhlner.csv2chart.model.DataModel;

public class HeatChartFactory extends AbstractChartFactory {

	private static final Color PASTEL_BLUE = new Color(0x2166ac);
	private static final Color PASTEL_YELLOW = new Color(0xffffbf);
	private static final Color PASTEL_RED = new Color(0xb2182b);
	

	@Override
	public JFreeChart createChart(Data data, DataModel dataModel, Parameters parameters) {
		XYZDataset dataset = createXYZDataset(data, parameters);

		NumberAxis xAxis = new NumberAxis(parameters.xAxisLabel);
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = new NumberAxis(parameters.yAxisLabel);
		yAxis.setAutoRangeIncludesZero(false);

		XYPlot plot = new XYPlot(dataset, yAxis, xAxis, null);

		XYBlockRenderer renderer = new XYBlockRenderer();

		PaintScale paintScale;
		if (parameters.colorScaleMidValue == null) {
			if (parameters.colorScaleMidColor != null && parameters.colorScaleMinValue >= 0) {
				parameters.colorScaleMidValue = (parameters.colorScaleMaxValue - parameters.colorScaleMinValue) / 2 + parameters.colorScaleMinValue;
			} else {
				parameters.colorScaleMidValue = 0.0;
			}
		}
		
		if (parameters.colorScaleMinColor == null) {
			parameters.colorScaleMinColor = PASTEL_BLUE;
		}
		if (parameters.colorScaleMidColor == null) {
			parameters.colorScaleMidColor = PASTEL_YELLOW;
		}
		if (parameters.colorScaleMaxColor == null) {
			parameters.colorScaleMaxColor = PASTEL_RED;
		}
		if (parameters.colorScaleDefaultColor == null) {
			parameters.colorScaleDefaultColor = Color.LIGHT_GRAY;
		}

		if (parameters.colorScaleMinValue < parameters.colorScaleMidValue && parameters.colorScaleMaxValue > parameters.colorScaleMidValue) {
			paintScale = new ThreeColorPaintScale(
					parameters.colorScaleMinValue,
					parameters.colorScaleMidValue,
					parameters.colorScaleMaxValue,
					parameters.colorScaleMinColor,
					parameters.colorScaleMidColor,
					parameters.colorScaleMaxColor,
					parameters.colorScaleDefaultColor);
		} else if (parameters.colorScaleMinValue < parameters.colorScaleMidValue) {
			paintScale = new TwoColorPaintScale(
					parameters.colorScaleMinValue,
					parameters.colorScaleMaxValue,
					parameters.colorScaleMidColor,
					parameters.colorScaleMinColor,
					parameters.colorScaleDefaultColor);
		} else {
			paintScale = new TwoColorPaintScale(
					parameters.colorScaleMinValue,
					parameters.colorScaleMaxValue,
					parameters.colorScaleMidColor,
					parameters.colorScaleMaxColor,
					parameters.colorScaleDefaultColor);
		}

		renderer.setPaintScale(paintScale);
		plot.setRenderer(renderer);
		plot.setOrientation(PlotOrientation.HORIZONTAL);

		JFreeChart chart = new JFreeChart(parameters.title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);

		boolean legend = true;
		if (legend) {
			NumberAxis scaleAxis = new NumberAxis(parameters.zAxisLabel);
	        scaleAxis.setAxisLinePaint(Color.WHITE);
	        scaleAxis.setTickMarkPaint(Color.WHITE);
	        scaleAxis.setTickLabelFont(new Font("Dialog", Font.PLAIN, 7));
	        scaleAxis.setRange(parameters.colorScaleMinValue, parameters.colorScaleMaxValue);
	        
			PaintScaleLegend scaleLegend = new PaintScaleLegend(paintScale, scaleAxis);
			scaleLegend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
			scaleLegend.setAxisOffset(5.0);
			scaleLegend.setMargin(new RectangleInsets(5, 5, 5, 5));
			scaleLegend.setPadding(new RectangleInsets(10, 10, 10, 10));
			scaleLegend.setStripWidth(10);
			scaleLegend.setPosition(RectangleEdge.RIGHT);
			chart.addSubtitle(scaleLegend);
		}
		
		return chart;
	}

	private XYZDataset createXYZDataset(Data data, Parameters parameters) {
		List<List<String>> rows = data.getRows();
		
		DefaultXYZDataset dataset = new DefaultXYZDataset();
		
		int headerColumnCount = parameters.headerColumn ? 1 : 0;
		
		int headerRowCount = data.getHeaderRowCount();
		if (headerRowCount == 0 && parameters.headerRow) {
			headerRowCount = 1;
		}

		double[] columnHeaders = new double[rows.get(0).size() - headerColumnCount]; 
		if (headerRowCount > 0) {
			List<String> row = rows.get(0);
			for (int columnIndex = headerColumnCount; columnIndex < row.size(); columnIndex++) {
				double value = CsvDataLoader.toDouble(row.get(columnIndex));
				columnHeaders[columnIndex - headerColumnCount] = value;
			}			
		} else {
			for (int i = 0; i < columnHeaders.length; i++) {
				columnHeaders[i] = i;
			}
		}
		
		double[] rowHeaders = new double[rows.size() - headerRowCount];
		if (headerColumnCount > 0) {
			for (int rowIndex = headerRowCount; rowIndex < rows.size(); rowIndex++) {
				List<String> row = rows.get(rowIndex);
				double value = CsvDataLoader.toDouble(row.get(0));
				rowHeaders[rowIndex - headerRowCount] = value;
			}			
		} else {
			for (int i = 0; i < rowHeaders.length; i++) {
				rowHeaders[i] = i;
			}
		}
		
		int n = rowHeaders.length * columnHeaders.length;
		double[] xValues = new double[n];
		double[] yValues = new double[n];
		double[] zValues = new double[n];
		
		double minValue = Double.MAX_VALUE;
		double maxValue = -Double.MAX_VALUE;
		
		int index = 0;
		for (int rowIndex = 0; rowIndex < rowHeaders.length; rowIndex++) {
			List<String> row = rows.get(rowIndex + headerRowCount);
			for (int columnIndex = 0; columnIndex < columnHeaders.length; columnIndex++) {
				double value = CsvDataLoader.toDouble(row.get(columnIndex + headerColumnCount));
				
				xValues[index] = rowHeaders[rowIndex];
				yValues[index] = columnHeaders[columnIndex];
				zValues[index] = value;
				
				minValue = Math.min(minValue, value);
				maxValue = Math.max(maxValue, value);
				
				index++;
			}
		}
		
		dataset.addSeries("series", new double[][] { xValues, yValues, zValues });
		
		if (parameters.colorScaleMinValue == null) {
			parameters.colorScaleMinValue = minValue;
		}
		if (parameters.colorScaleMaxValue == null) {
			parameters.colorScaleMaxValue = maxValue;
		}

		return dataset;
	}
}