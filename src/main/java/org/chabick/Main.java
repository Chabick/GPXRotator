package org.chabick;

import com.raylib.Colors;
import jnafilechooser.api.JnaFileChooser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import com.raylib.Raylib;
import org.jsoup.nodes.Element;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Main {
    static Document doc;

    public static String byteListToString(List<Byte> l, Charset charset) {
        if (l == null) {
            return "";
        }
        byte[] array = new byte[l.size()];
        int i = 0;
        for (Byte current : l) {
            array[i] = current;
            i++;
        }
        return new String(array, charset);
    }

    public static void main(String[] args) {

        /*JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);*/




        Raylib.SetTraceLogLevel(5);
        Raylib.InitWindow(800, 450, "GPXRotator");
        Raylib.GuiSetStyle(Raylib.TEXTBOX, Raylib.BASE_COLOR_NORMAL, 0xd3d3d3);
        Raylib.GuiSetStyle(Raylib.TEXTBOX, Raylib.BASE_COLOR_FOCUSED, 0xd3d3d3);
        Raylib.GuiSetStyle(Raylib.TEXTBOX, Raylib.BASE_COLOR_PRESSED, 0xe3e3e3);
        Raylib.GuiSetStyle(Raylib.BUTTON, Raylib.BASE_COLOR_FOCUSED, 0xd3d3d3);

        File f = null;
        JnaFileChooser fc = new JnaFileChooser();
        fc.addFilter("All Files", "*");
        fc.addFilter("GPX", "gpx");
        if (fc.showOpenDialog(null)) {
            f = fc.getSelectedFile();
        }

        if (f == null) {
            Raylib.CloseWindow();
            return;
        }

        String l = "ERROR";
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            l = sb.toString();
            br.close();
        } catch (Exception e) {}

        Document doc = null;
        try {
            doc = Jsoup.parse(f);
        } catch (IOException e) {}

        if (doc == null) {
            Raylib.BeginDrawing();
            Raylib.DrawText("There was an ERROR while loading this file. Please try again.", 1, 1, 25, Colors.RED);
            Raylib.EndDrawing();
            try {Thread.sleep(5000);} catch (InterruptedException e) {}
            Raylib.CloseWindow();
            return;
        }

        ArrayList<Element> tracks = new ArrayList<>();
        doc.getElementsByTag("trk").forEach(tracks::add);
        ArrayList<String> trackNames = new ArrayList<>();
        tracks.forEach(track -> trackNames.add(track.getElementsByTag("name").text()));
        ArrayList<Integer> trackSizes = new ArrayList<>();
        tracks.forEach(track -> {trackSizes.add(track.getElementsByTag("trkpt").size());});

        Element bounds = doc.getElementsByTag("bounds").first();

        while (!Raylib.WindowShouldClose()) {
            Raylib.BeginDrawing();
            Raylib.ClearBackground(Colors.RAYWHITE);
            //Raylib.DrawText(l, 1, 1, 1, Colors.BLACK); //x, y, size
            Raylib.DrawText("Tracks found: " + trackNames.size(), 1, 1, 20, Colors.BLACK);
            for (int i = 1; i <= trackNames.size(); i++) {
                Raylib.DrawRectangle(1, 2+22*i, Raylib.MeasureText("-" + trackNames.get(i-1) + " (" +
                        trackSizes.get(i-1) + ")", 20), 20, Colors.LIGHTGRAY);
                Raylib.DrawText("-" + trackNames.get(i-1) + " (" +
                        trackSizes.get(i-1) + ")", 1, 1+22*i, 20, Colors.BLACK);
            }

            Raylib.EndDrawing();

            if(Raylib.IsMouseButtonDown(Raylib.MOUSE_BUTTON_LEFT)) {
                int y = (int) (Math.floor(((double)(Raylib.GetMouseY()-1))/22.0d)) - 1;
                int x = Raylib.GetMouseX()-1;

                if (y < trackNames.size() && y > -1) {
                    String name = trackNames.get(y);
                    if (x <= Raylib.MeasureText(name, 20)) {
                        boolean r_st = true;
                        boolean reverse = false;
                        ByteBuffer byteInput = ByteBuffer.allocate(100);

                        while (r_st) {
                            Raylib.BeginDrawing();


                            Raylib.ClearBackground(Colors.RAYWHITE);
                            Raylib.DrawText("Selected Track: " + name + " (" +
                                    trackSizes.get(y) + ")", 1, 1, 20, Colors.BLACK);

                            if (Raylib.GuiButton(new Raylib.Rectangle().x(750).y(0).width(50).height(25), "Back") == 1)
                                r_st = false;

                            Raylib.GuiTextBox(new Raylib.Rectangle().x(1).y(30).width(200).height(25),
                                    byteInput, 20, true);

                            Raylib.DrawText("The trackname will be automatically changed to the filename.", 1, 100, 10, Colors.BLACK);

                            //System.out.println(new String(textInput.array(), Charset.defaultCharset()));
                            //System.out.println(textInput.get(0));
                            //ArrayList<Byte> byteList = new ArrayList<Byte>(Arrays.ofbyteInput.array());
                            List<Byte> list = IntStream.range(0, byteInput.array().length).mapToObj(i -> byteInput.array()[i])
                                    .collect(Collectors.toList());
                            list.removeIf(b -> {return b.byteValue() == (byte) 0;});

                            if (Raylib.GuiButton(new Raylib.Rectangle().x(1).y(60).width(90).height(25), "Reverse: " + reverse) == 1) {
                                reverse = !reverse;
                                if (list.isEmpty()) {
                                    byteInput.put(0, "0".getBytes()[0]);
                                    System.out.println(byteInput.array().toString());
                                }
                            }

                            if (!list.isEmpty()) {
                                String textInput = byteListToString(list, Charset.defaultCharset());
                                try {
                                    int shift = Integer.valueOf(textInput);

                                    if (Raylib.GuiButton(new Raylib.Rectangle().x(210).y(30).width(100).height(25), "Convert/Shift") == 1) {
                                        List<Element> trackPoints = tracks.get(y).getElementsByTag("trkpt");
                                        for (int i = 0; i < shift; i++) {
                                            trackPoints.addFirst(trackPoints.getLast());
                                            trackPoints.removeLast();
                                        }
                                        if (reverse) trackPoints = trackPoints.reversed();

                                        File saveFile = null;
                                        JnaFileChooser saveChooser = new JnaFileChooser();
                                        saveChooser.addFilter("All Files", "*");
                                        if (saveChooser.showSaveDialog(null)) {
                                            saveFile = saveChooser.getSelectedFile();
                                        }

                                        if (saveFile == null) {
                                            throw new RuntimeException("No save file selected");
                                        }

                                        BufferedWriter w = null;
                                        try {
                                            w = new BufferedWriter(new FileWriter(saveFile));

                                            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n");
                                            w.write("<gpx \n");
                                            w.write("version=\"1.0\" creator=\"GpxRotator\" \n");
                                            w.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
                                            w.write("xmlns=\"http://www.topografix.com/GPX/1/0\" \n");
                                            w.write("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\"> \n");
                                            w.write("<time>"+System.currentTimeMillis()+"</time> \n");

                                            //Gpx Track data write
                                            w.write("<bounds ");
                                            for (Attribute attr : bounds.attributes().asList()) {
                                                w.write(attr.getKey() + "=\"" + attr.getValue() + "\" ");
                                            }
                                            w.write("/>\n");

                                            w.write("<trk>\n");
                                            w.write("<name>" + saveFile.getName().replaceAll("\\.gpx", "") + "</name>\n");
                                            w.write("<trkseg>\n");

                                            for (Element el : trackPoints) {
                                                w.write("<trkpt ");
                                                for (Attribute attr : el.attributes().asList()) {
                                                    w.write(attr.getKey() + "=\"" + attr.getValue() + "\" ");
                                                }
                                                w.write(">\n");
                                                w.write("<ele>" + el.getElementsByTag("ele").getFirst().text() + "</ele>\n");
                                                w.write("</trkpt>\n");
                                            }

                                            w.write("</trkseg>\n");
                                            w.write("</trk>\n");
                                            w.write("</gpx> \n");

                                            w.close();

                                        } catch (Exception e) {}

                                        Raylib.CloseWindow();
                                        return;

                                    }
                                } catch(NumberFormatException e) {
                                    Raylib.DrawText("Only Numbers are allowed!", 1, 85, 10, Colors.RED);
                                }
                            }

                            Raylib.EndDrawing();


                            if (Raylib.WindowShouldClose()) {
                                Raylib.CloseWindow();
                                return;
                            }
                        }
                    }
                }
            }
        }
        Raylib.CloseWindow();
    }
}