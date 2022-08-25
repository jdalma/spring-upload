package hello.upload;

import hello.upload.domain.Item;
import hello.upload.domain.ItemRepository;
import hello.upload.domain.UploadFile;
import hello.upload.file.FileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemRepository repository;
    private final FileStore fileStore;

    @GetMapping("/items/new")
    public String newItem(@ModelAttribute ItemForm form){
        return "item-form";
    }

    @PostMapping("/items/new")
    public String saveItem(@ModelAttribute ItemForm form , RedirectAttributes redirectAttributes) throws IOException {
        UploadFile attachFile = fileStore.storeFile(form.getAttachFile());
        List<UploadFile> storeImageFiles = fileStore.storeFiles(form.getImageFiles());

        // 데이터 베이스에 저장
        Item item = new Item();
        item.setItemName(form.getItemName());
        item.setAttachFile(attachFile);
        item.setImageFiles(storeImageFiles);
        repository.save(item);

        redirectAttributes.addAttribute("itemId" , item.getId());

        return "redirect:/items/{itemId}";
    }

    @GetMapping("/items/{id}")
    public String items(@PathVariable Long id , Model model){
        model.addAttribute("item" , repository.findById(id));
        return "item-view";
    }

    @GetMapping("/images/{filename}")
    @ResponseBody
    public Resource downloadImage(@PathVariable String filename) throws MalformedURLException {
        return new UrlResource("file:" + fileStore.getFullPath(filename));
    }

    @GetMapping("/attach/{itemId}")
    public ResponseEntity<Resource> downloadAttach(@PathVariable Long itemId) throws MalformedURLException {
        Item item = repository.findById(itemId);
        String storeFileName = item.getAttachFile().getStoreFileName();
        String uploadFileName = item.getAttachFile().getUploadFileName();

        UrlResource urlResource = new UrlResource("file:" + fileStore.getFullPath(storeFileName));
        log.info("uploadFileName = {}" , uploadFileName);

        // 헤더를 지정해주지 않으면 순수 파일의 내용을 전달한다
        // return ResponseEntity.ok().body(urlResource);

        // UriUtils 스프링에서 인코딩 관련 지원
        String encodeUploadFileName = UriUtils.encode(uploadFileName , StandardCharsets.UTF_8); // 제목 깨짊 방지
        String contentDisposition = "attachment; filename=\"" + encodeUploadFileName + "\"";

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION , contentDisposition)
                .body(urlResource);
    }
}
