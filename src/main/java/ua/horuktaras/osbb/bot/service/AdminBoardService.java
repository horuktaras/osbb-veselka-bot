package ua.horuktaras.osbb.bot.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ua.horuktaras.osbb.bot.model.entity.Request;
import ua.horuktaras.osbb.bot.model.enums.RequestStatus;
import ua.horuktaras.osbb.bot.repository.AdminUserRepository;
import ua.horuktaras.osbb.bot.repository.RequestRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminBoardService {

    public record BoardMessage(String text, InlineKeyboardMarkup keyboard) {}

    private static final int PAGE_SIZE = 5;
    private static final DateTimeFormatter COMPACT_FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AdminUserRepository adminUserRepository;
    private final RequestRepository requestRepository;

    public AdminBoardService(AdminUserRepository adminUserRepository,
                             RequestRepository requestRepository) {
        this.adminUserRepository = adminUserRepository;
        this.requestRepository = requestRepository;
    }

    public boolean isAdmin(Long telegramId) {
        return adminUserRepository.existsByTelegramId(telegramId);
    }

    public BoardMessage buildBoardMessage(int page, String filterStatus) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        List<Request> allRequests;
        if ("ALL".equals(filterStatus)) {
            allRequests = requestRepository.findAll(sort);
        } else {
            RequestStatus status = RequestStatus.valueOf(filterStatus);
            allRequests = requestRepository.findByStatus(status, sort);
        }

        int total = allRequests.size();
        int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        int fromIndex = safePage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, total);
        List<Request> pageRequests = allRequests.subList(fromIndex, toIndex);

        String filterLabel = "ALL".equals(filterStatus) ? "Всі" : RequestStatus.valueOf(filterStatus).getDisplayName();

        StringBuilder sb = new StringBuilder();
        sb.append("📋 <b>Заявки ОСББ</b>\n\n");
        sb.append("Сторінка ").append(safePage + 1).append(" з ").append(totalPages)
                .append(" | Всього: ").append(total).append("\n");
        sb.append("Фільтр: ").append(filterLabel).append("\n\n");
        sb.append("──────────────────\n");

        for (Request r : pageRequests) {
            String urgencyMark = r.isUrgent() ? " ❗" : "";
            String createdStr = r.getCreatedAt() != null ? r.getCreatedAt().format(COMPACT_FMT) : "";
            sb.append("#").append(r.getId()).append(" | ")
                    .append(r.getType().getDisplayName()).append(urgencyMark)
                    .append(" | ").append(createdStr).append("\n");
            String author = r.getName() != null ? r.getName() : (r.getTelegramUsername() != null ? "@" + r.getTelegramUsername() : "");
            sb.append("👤 ").append(escapeHtml(author))
                    .append(" | ").append(r.getStatus().getDisplayName()).append("\n\n");
        }
        sb.append("──────────────────");

        InlineKeyboardMarkup keyboard = buildListKeyboard(pageRequests, safePage, totalPages, filterStatus);
        return new BoardMessage(sb.toString(), keyboard);
    }

    public BoardMessage buildDetailMessage(Request request, int returnPage, String returnFilter) {
        String urgencyMark = request.isUrgent() ? "❗ Так" : "Ні";
        String userMention = request.getTelegramUsername() != null
                ? "@" + request.getTelegramUsername()
                : "id:" + request.getTelegramUserId();
        String createdStr = request.getCreatedAt() != null ? request.getCreatedAt().format(FULL_FMT) : "";

        String text = "<b>📋 Заявка #" + request.getId() + "</b>\n\n"
                + "👤 <b>Ім'я:</b> " + escapeHtml(request.getName()) + "\n"
                + "📞 <b>Контакт:</b> " + escapeHtml(request.getContact()) + "\n"
                + "⚡ <b>Терміново:</b> " + urgencyMark + "\n"
                + "🗂 <b>Тип:</b> " + request.getType().getDisplayName() + "\n"
                + "📝 <b>Опис:</b> " + escapeHtml(request.getDescription()) + "\n"
                + "🙍 <b>Від:</b> " + userMention + "\n"
                + "📅 <b>Подано:</b> " + createdStr + "\n\n"
                + "Статус: " + request.getStatus().getDisplayName();

        InlineKeyboardMarkup keyboard = buildDetailKeyboard(request, returnPage, returnFilter);
        return new BoardMessage(text, keyboard);
    }

    private InlineKeyboardMarkup buildListKeyboard(List<Request> pageRequests, int page, int totalPages, String filterStatus) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        // One button per request
        for (Request r : pageRequests) {
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text("📋 Заявка #" + r.getId())
                    .callbackData("board:v:" + r.getId() + ":" + page + ":" + filterStatus)
                    .build();
            rows.add(new InlineKeyboardRow(List.of(btn)));
        }

        // Status filter counts
        long countNew = requestRepository.countByStatus(RequestStatus.NEW);
        long countProcessed = requestRepository.countByStatus(RequestStatus.PROCESSED);
        long countInProgressOsbb = requestRepository.countByStatus(RequestStatus.IN_PROGRESS_OSBB);
        long countInProgressContractor = requestRepository.countByStatus(RequestStatus.IN_PROGRESS_CONTRACTOR);
        long countVerification = requestRepository.countByStatus(RequestStatus.VERIFICATION);
        long countClosed = requestRepository.countByStatus(RequestStatus.CLOSED);
        long countAll = requestRepository.count();

        // Filter rows: 3 per row
        List<InlineKeyboardButton> filterRow1 = new ArrayList<>();
        filterRow1.add(filterButton("NEW".equals(filterStatus), "🆕 Нові (" + countNew + ")", "board:0:NEW"));
        filterRow1.add(filterButton("PROCESSED".equals(filterStatus), "✅ Опрацьовані (" + countProcessed + ")", "board:0:PROCESSED"));
        filterRow1.add(filterButton("IN_PROGRESS_OSBB".equals(filterStatus), "🔧 В роботі ОСББ (" + countInProgressOsbb + ")", "board:0:IN_PROGRESS_OSBB"));
        rows.add(new InlineKeyboardRow(filterRow1));

        List<InlineKeyboardButton> filterRow2 = new ArrayList<>();
        filterRow2.add(filterButton("IN_PROGRESS_CONTRACTOR".equals(filterStatus), "🏢 Підрядник (" + countInProgressContractor + ")", "board:0:IN_PROGRESS_CONTRACTOR"));
        filterRow2.add(filterButton("VERIFICATION".equals(filterStatus), "🔍 Перевірка (" + countVerification + ")", "board:0:VERIFICATION"));
        filterRow2.add(filterButton("CLOSED".equals(filterStatus), "🔒 Закриті (" + countClosed + ")", "board:0:CLOSED"));
        rows.add(new InlineKeyboardRow(filterRow2));

        List<InlineKeyboardButton> filterRow3 = new ArrayList<>();
        filterRow3.add(filterButton("ALL".equals(filterStatus), "Всі (" + countAll + ")", "board:0:ALL"));
        rows.add(new InlineKeyboardRow(filterRow3));

        // Navigation row
        InlineKeyboardButton prevBtn;
        if (page == 0) {
            prevBtn = InlineKeyboardButton.builder().text("◀").callbackData("board:noop").build();
        } else {
            prevBtn = InlineKeyboardButton.builder().text("◀").callbackData("board:" + (page - 1) + ":" + filterStatus).build();
        }
        InlineKeyboardButton pageBtn = InlineKeyboardButton.builder()
                .text((page + 1) + " / " + totalPages)
                .callbackData("board:noop")
                .build();
        InlineKeyboardButton nextBtn;
        if (page >= totalPages - 1) {
            nextBtn = InlineKeyboardButton.builder().text("▶").callbackData("board:noop").build();
        } else {
            nextBtn = InlineKeyboardButton.builder().text("▶").callbackData("board:" + (page + 1) + ":" + filterStatus).build();
        }
        rows.add(new InlineKeyboardRow(List.of(prevBtn, pageBtn, nextBtn)));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton filterButton(boolean active, String label, String callbackData) {
        String text = active ? "✓ " + label : label;
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }

    private InlineKeyboardMarkup buildDetailKeyboard(Request request, int returnPage, String returnFilter) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        List<RequestStatus> nextStatuses = request.getStatus().nextStatuses();

        if (!nextStatuses.isEmpty()) {
            List<InlineKeyboardButton> currentRow = new ArrayList<>();
            for (int i = 0; i < nextStatuses.size(); i++) {
                RequestStatus status = nextStatuses.get(i);
                InlineKeyboardButton button = InlineKeyboardButton.builder()
                        .text(status.getDisplayName())
                        .callbackData("status:" + request.getId() + ":" + status.name())
                        .build();
                currentRow.add(button);
                if (currentRow.size() == 2 || i == nextStatuses.size() - 1) {
                    rows.add(new InlineKeyboardRow(currentRow));
                    currentRow = new ArrayList<>();
                }
            }
        }

        InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
                .text("← До списку")
                .callbackData("board:" + returnPage + ":" + returnFilter)
                .build();
        rows.add(new InlineKeyboardRow(List.of(backBtn)));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
