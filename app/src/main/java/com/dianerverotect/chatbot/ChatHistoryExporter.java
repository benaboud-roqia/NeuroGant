package com.dianerverotect.chatbot;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatHistoryExporter {
    private static final String TAG = "ChatHistoryExporter";
    private static final Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

    public static File exportToPdf(Context context, List<ChatHistoryItem> items, String title) throws IOException, DocumentException {
        // Créer le dossier de destination s'il n'existe pas
        File pdfFolder = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ChatHistory");
        if (!pdfFolder.exists()) {
            pdfFolder.mkdirs();
        }

        // Créer le fichier PDF avec un nom unique basé sur la date
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File pdfFile = new File(pdfFolder, "ChatHistory_" + timeStamp + ".pdf");

        // Créer le document PDF
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
        document.open();

        // Ajouter le titre
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(20);
        document.add(titleParagraph);

        // Créer le tableau
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 3, 2, 5});

        // Ajouter les en-têtes
        addTableHeader(table);

        // Ajouter les données
        for (ChatHistoryItem item : items) {
            addTableRow(table, item);
        }

        document.add(table);
        document.close();

        return pdfFile;
    }

    private static void addTableHeader(PdfPTable table) {
        String[] headers = {"Date", "Type", "Patient", "Message"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private static void addTableRow(PdfPTable table, ChatHistoryItem item) {
        addCell(table, item.getDateHeure());
        addCell(table, item.getTypeEchange());
        addCell(table, item.getPatientId());
        addCell(table, item.getResume());
    }

    private static void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, normalFont));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(5);
        table.addCell(cell);
    }
} 