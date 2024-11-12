package sample.cafekiosk.spring.api.service.order;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import sample.cafekiosk.spring.client.mail.MailSendClient;
import sample.cafekiosk.spring.domain.history.mail.MailSendHistory;
import sample.cafekiosk.spring.domain.history.mail.MailSendHistoryRepository;
import sample.cafekiosk.spring.domain.order.Order;
import sample.cafekiosk.spring.domain.order.OrderRepository;
import sample.cafekiosk.spring.domain.orderproduct.OrderProductRepository;
import sample.cafekiosk.spring.domain.product.Product;
import sample.cafekiosk.spring.domain.product.ProductRepository;
import sample.cafekiosk.spring.domain.product.ProductType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static sample.cafekiosk.spring.domain.order.OrderStatus.PAYMENT_COMPLETED;
import static sample.cafekiosk.spring.domain.product.ProductSellingStatus.SELLING;
import static sample.cafekiosk.spring.domain.product.ProductType.HANDMADE;

@SpringBootTest
class OrderStatisticsServiceTest {

    @Autowired
    private OrderStatisticsService orderStatisticsService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderProductRepository orderProductRepository;

    @Autowired
    MailSendHistoryRepository mailSendHistoryRepository;

    @MockBean
    MailSendClient mailSendClient;

    @AfterEach
    void tearDown() {
        orderProductRepository.deleteAllInBatch(); // todo 먼저 삭제해야 하는 이유 설명 예정
        orderRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        mailSendHistoryRepository.deleteAllInBatch();
    }

    @DisplayName("결제 완료 주문들을 조회하여 매출 통계 메일을 전송한다.")
    @Test
    void sendOrderStatisticsMail() {
        //given
        LocalDateTime now = LocalDateTime.of(2024, 11, 12, 0, 0);

        Product product1 = createProduct("001", HANDMADE, 1000);
        Product product2 = createProduct("002", HANDMADE, 2000);
        Product product3 = createProduct("003", HANDMADE, 3000);
        List<Product> products = List.of(product1, product2, product3);
        productRepository.saveAll(products);

        Order order1 = createPaymentCompletedOrder(LocalDateTime.of(2024, 11, 11, 23, 59, 59), products);
        Order order2 = createPaymentCompletedOrder(now, products);
        Order order3 = createPaymentCompletedOrder(LocalDateTime.of(2024, 11, 12, 23, 59, 59), products);
        Order order4 = createPaymentCompletedOrder(LocalDateTime.of(2024, 11, 13, 0, 0), products);

        //stubbing
        when(mailSendClient.sendEmail(any(String.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn(true);

        //when
        boolean result = orderStatisticsService.sendOrderStatisticsMail(LocalDate.of(2024, 11, 12), "test@test.com");

        //then
        assertThat(result).isTrue();

        List<MailSendHistory> histories = mailSendHistoryRepository.findAll();
        assertThat(histories).hasSize(1)
                .extracting("content")
                .containsExactlyInAnyOrder("총 매출 합계는 12000원입니다.");
    }

    private Product createProduct(String productNumber, ProductType type, int price) {
        return Product.builder()
                .productNumber(productNumber)
                .type(type)
                .sellingStatus(SELLING)
                .name("메뉴 이름")
                .price(price)
                .build();
    }

    private Order createPaymentCompletedOrder(LocalDateTime now, List<Product> products) {
        Order order = Order.builder()
                .orderStatus(PAYMENT_COMPLETED)
                .registeredDateTime(now)
                .products(products)
                .build();
        return orderRepository.save(order);
    }
}
