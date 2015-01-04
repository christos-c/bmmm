package tagInducer.helpers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import utils.StringCoder;

/**
 * Visualisation class for the various features used in <code>Corpus</code>.
 * Best used when combined by gold PoS tags.
 * @author Christos Christodoulopoulos
 */
public class FeatureViewer extends JFrame {
	private static final long serialVersionUID = 3798919692979332710L;
	private DrawPanel panel;

	public FeatureViewer(StringCoder coder, int [][] features, String[] fNames){
		super("Feature viewer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(true);
		panel = new DrawPanel(coder, features, fNames);
		ScaleHandler scaler = new ScaleHandler();
		panel.addKeyListener(scaler);
		panel.addMouseWheelListener(scaler);
		TranslateHandler translater = new TranslateHandler();
		panel.addMouseListener(translater);
		panel.addMouseMotionListener(translater);
		JScrollPane scroller = new JScrollPane(panel);
		add(scroller);
		setSize(500,500);
		setVisible(true);
	}


	private class DrawPanel extends JComponent {
		private static final long serialVersionUID = -4223112722968451514L;
		private StringCoder c;
		private int[][] feats;
		private String[] fNames;
		
		private double translateX;
		private double translateY;
		private double scale;
		private int drawThresh = 2;

		public DrawPanel(StringCoder coder, int [][] features, String[] fNames){
			c = coder;
			feats = features;
			this.fNames = fNames;
			setBackground(Color.WHITE);
			translateX = 0;
			translateY = 0;
			scale = 1;
			setOpaque(true);
			setDoubleBuffered(true);
			setFocusable(true); 
		}
		
		@Override public void paint(Graphics g){
			AffineTransform tx = new AffineTransform();
			tx.translate(translateX, translateY);
			tx.scale(scale, scale);
			
			int posX = 25;
			int posY = 50;
			//int size = 10;
			int posXRel = 0;
			int maxSize = 25;
			int step = 0;
			
			//super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.setTransform(tx);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
			//Use this to find the global maximum
			int max = 0;
			for (int[] words:feats){
				for (int feat:words){
					if (feat > max) max = feat;
				}
			}
			
			//TODO: Draw the feature names
			posXRel = posX+100+maxSize;
			g2.setPaint(Color.black);
			for (int i = 0; i < feats[0].length; i++) {
				AffineTransform oldTransform = g2.getTransform();
				g2.transform(AffineTransform.getRotateInstance(-Math.PI/2));
				g2.drawString(fNames[i], -35, posXRel-(maxSize/2)+2);
				g2.setTransform(oldTransform);
				posXRel += maxSize;
			}
			
			//Now paint the rows
			for (int wordType = 0; wordType < feats.length; wordType++){
				//Use this for local maximum
				/*int max = 0;
				for (int feat:feats[wordType]){
					if (feat > max) max = feat;
				}*/
				//Paint the text (word type)
				g2.setPaint(Color.black);
				g2.drawString(c.decode(wordType), posX, posY+(maxSize/2)+3);
				//Paint the boxes
				posXRel = posX+100;
				for (int feat:feats[wordType]){
					//Use logs for scaling
					int fSize = (int) (Math.log(feat)*maxSize/Math.log(max));
					//Calculate the colour
					/*int fCol = 255 - (feat*255/max);
					if (feat==0) g2.setPaint(getBackground());
					else g2.setPaint(new Color(fCol, 0, 0));
					*/
					//Calculate the offset
					int offset = (maxSize-fSize)/2;
					if (feat > drawThresh) g2.fill(new Rectangle2D.Double(posXRel+offset, posY+offset, fSize, fSize));
					posXRel += maxSize + step;
				}
				g2.setPaint(Color.LIGHT_GRAY);
				g2.drawLine(0, posY, posXRel, posY);
				posY += maxSize;
			}
			
			//Paint the column lines
			g2.setPaint(Color.LIGHT_GRAY);
			posXRel = 125;
			for (int featType = 0; featType <= feats[0].length; featType++){
				g2.drawLine(posXRel, 0, posXRel, posY);
				posXRel += maxSize + step;
			}
		}
	}
	
	private class TranslateHandler implements MouseListener, MouseMotionListener {
		private int lastOffsetX;
		private int lastOffsetY;

		public void mousePressed(MouseEvent e) {
			// capture starting point
			lastOffsetX = e.getX();
			lastOffsetY = e.getY();
		}

		public void mouseDragged(MouseEvent e) {
			// new x and y are defined by current mouse location subtracted
			// by previously processed mouse location
			int newX = e.getX() - lastOffsetX;
			int newY = e.getY() - lastOffsetY;

			// increment last offset to last processed by drag event.
			lastOffsetX += newX;
			lastOffsetY += newY;

			// update the canvas locations
			panel.translateX += newX;
			panel.translateY += newY;

			// schedule a repaint.
			panel.repaint();
		}

		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseMoved(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
	}

	private class ScaleHandler implements MouseWheelListener, KeyListener {
		private int lastOffsetY =  0;
		private boolean zoom = false;
		public void mouseWheelMoved(MouseWheelEvent e) {
			if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
				if (zoom){
					// make it a reasonable amount of zoom
					// .1 gives a nice slow transition
					panel.scale -= (.1 * e.getWheelRotation());
					// don't cross negative threshold.
					// also, setting scale to 0 has bad effects
					panel.scale = Math.max(0.00001, panel.scale); 
				}
				else {
					int newY = -20*e.getWheelRotation() - lastOffsetY;
					panel.translateY += newY;
				}
				panel.repaint();
			}
		}
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_CONTROL) zoom = true;
		}
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_CONTROL) zoom = false;
		}
		public void keyTyped(KeyEvent e) {}
}
	
	public static void main(String[] args){
		StringCoder coder = new StringCoder();
		coder.encode("Test1");
		coder.encode("Test2");
		int [][] feats = new int[2][3];
		int [] vect1 = {100, 34, 2};
		int [] vect2 = {2, 45, 126};
		feats[coder.encode("Test1")] = vect1;
		feats[coder.encode("Test2")] = vect2;
		String[] featNames = {"NNP", "PRP", "ADJ"};
		new FeatureViewer(coder, feats, featNames);
		/*Sample code (to use inside Corpus.java):
		//Visualisation
		//Generate the feature names
		String[] featureNames = new String[features[0].length];
		for (int i = 0; i < featureNames.length; i++) {
			if (tagsCoder.decode(i)==null) featureNames[i] = "ROOT";
			else featureNames[i] = tagsCoder.decode(i);
		}
		
		String[] poss = {"VB", "NN"};
		for (String targetPOS:poss){
			StringCoder posWordCoder = new StringCoder();
			List<int[]> posFeats = new ArrayList<int[]>();
			List<String> seenWords = new ArrayList<String>();
			for (int wordInd = 0; wordInd < words.length; wordInd++){
				String word = wordsCoder.decode(words[wordInd]);
				if (seenWords.contains(word)) continue;
				seenWords.add(word);
				String tag = tagsCoder.decode(goldTags[wordInd]);
				if (!tag.equals(targetPOS)) continue;
				posWordCoder.encode(word);
				posFeats.add(features[wordsCoder.encode(word)]);
			}
			//Turn the list into an array
			int[][] t = new int[posFeats.size()][features[0].length];
			for (int f = 0; f < posFeats.size(); f++){
				t[f] = posFeats.get(f);
			}
			new FeatureViewer(posWordCoder, t, featureNames);
		}
		*/
	}
}