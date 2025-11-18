package com.dreammapper.service;

import com.dreammapper.model.User;

public interface ExportService {

    record ExportResult(byte[] content, String filename, String contentType) {}

    ExportResult exportDreams(User user, String format);
}

